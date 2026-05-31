# Sonar 限界上下文（Bounded Contexts）

## 概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        Sonar System                             │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │  Recording   │  │ Transcription│  │      Analysis        │ │
│  │   Context    │  │   Context     │  │      Context         │ │
│  │              │──▶│              │──▶│                      │ │
│  │ · 录音管理   │  │ · 转写管理   │  │ · 问题提取           │ │
│  │ · 音频存储   │  │ · 分段管理   │  │ · 背景提取           │ │
│  │ · 元数据     │  │ · 语种检测   │  │ · 分析结果存储       │ │
│  └──────────────┘  └──────────────┘  └──────────────────────┘ │
│                                                   │            │
│                                                   ▼            │
│                          ┌────────────────────────────────────┐ │
│                          │         Solution Context          │ │
│                          │                                    │ │
│                          │ · 解决方案搜索                     │ │
│                          │ · 搜索结果聚合                     │ │
│                          │ · 报告生成                         │ │
│                          └────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Context 1：Recording（录音上下文）

### 职责
- 管理录音的完整生命周期（开始、停止、暂停、恢复）
- 处理音频文件的本地存储
- 维护录音元数据（时长、采样率、格式、创建时间）

### 领域模型

```
Recording（聚合根）
├── id: UUID
├── status: RecordingStatus（ Recording | Paused | Stopped ）
├── filePath: String
├── duration: Duration
├── sampleRate: Int
├── format: AudioFormat（ AAC | WAV | MP3 ）
├── createdAt: DateTime
└── metadata: RecordingMetadata（值对象）
    ├── title?: String
    ├── speakerCount?: Int
    └── tags?: List<String>

RecordingEvent（领域事件）
├── RecordingStarted
├── RecordingPaused
├── RecordingResumed
└── RecordingStopped
```

### 仓储接口
```
RecordingRepository（出站端口）
├── save(Recording)
├── findById(UUID): Recording?
├── findAll(): List<Recording>
└── delete(UUID)
```

---

## Context 2：Transcription（转写上下文）

### 职责
- 接收音频文件，调用 AI 服务进行语音转文字
- 管理转写结果（原始文本、时间戳分段）
- 处理多语言检测

### 领域模型

```
Transcription（聚合根）
├── id: UUID
├── recordingId: UUID（外部引用）
├── rawText: String
├── language: Language（值对象）
│   ├── code: String（BCP-47，如 "zh-CN"）
│   └── confidence: Double
├── segments: List<TranscriptSegment>（值对象）
│   ├── startTime: Duration
│   ├── endTime: Duration
│   └── text: String
├── status: TranscriptionStatus（ Pending | Processing | Completed | Failed ）
├── createdAt: DateTime
└── error?: String

TranscriptSegment（值对象）
├── startTime: Duration
├── endTime: Duration
├── text: String
└── speaker?: String?
```

### 仓储接口
```
TranscriptionRepository（出站端口）
├── save(Transcription)
├── findById(UUID): Transcription?
├── findByRecordingId(UUID): Transcription?
└── delete(UUID)
```

### 出站依赖
- **AI Provider Port**：调用外部语音识别服务（OpenAI Whisper 或其他）

---

## Context 3：Analysis（分析上下文）

### 职责
- 接收转写文本，调用大模型进行深度分析
- 从对话中提取核心问题列表
- 提取问题背景（上下文、在聊什么）
- 维护分析结果

### 领域模型

```
Analysis（聚合根）
├── id: UUID
├── transcriptionId: UUID（外部引用）
├── questions: List<ExtractedQuestion>（实体列表）
│   ├── id: UUID
│   ├── text: String
│   ├── type: QuestionType（ Specific | Open | Hypothetical ）
│   └── confidence: Double
├── background: ConversationBackground（值对象）
│   ├── summary: String
│   ├── topic: String
│   ├── participants: List<String>
│   └── contextDetails: String
├── status: AnalysisStatus（ Pending | Processing | Completed | Failed ）
├── createdAt: DateTime
└── error?: String

ExtractedQuestion（实体）
├── id: UUID
├── text: String
├── type: QuestionType
├── confidence: Double
└── relatedBackground?: String?
```

### 仓储接口
```
AnalysisRepository（出站端口）
├── save(Analysis)
├── findById(UUID): Analysis?
├── findByTranscriptionId(UUID): Analysis?
└── delete(UUID)
```

### 出站依赖
- **AI Provider Port**：调用 LLM 进行分析和提取

---

## Context 4：Solution（解决方案上下文）

### 职责
- 接收问题，搜索解决方案
- 聚合来自多个来源的搜索结果
- 生成结构化报告
- 导出/分享报告

### 领域模型

```
Solution（聚合根）
├── id: UUID
├── analysisId: UUID（外部引用）
├── questionId: UUID
├── sources: List<SolutionSource>（值对象列表）
│   ├── sourceName: String
│   ├── sourceUrl?: String
│   ├── content: String
│   └── relevanceScore: Double
├── synthesizedAnswer: String（值对象）
│   ├── answer: String
│   ├── confidence: Double
│   └── keyPoints: List<String>
├── status: SolutionStatus（ Pending | Searching | Completed | Failed ）
├── createdAt: DateTime
└── error?: String

Report（实体）
├── id: UUID
├── solutionIds: List<UUID>
├── title: String
├── content: ReportContent（值对象）
│   ├── summary: String
│   ├── sections: List<ReportSection>
│   └── recommendations: List<String>
├── format: ReportFormat（ Markdown | JSON | PDF ）
├── createdAt: DateTime
└── exportedAt?: DateTime
```

### 仓储接口
```
SolutionRepository（出站端口）
├── save(Solution)
├── findById(UUID): Solution?
├── findByAnalysisId(UUID): List<Solution>
└── delete(UUID)

ReportRepository（出站端口）
├── save(Report)
├── findById(UUID): Report?
├── findAll(): List<Report>
└── delete(UUID)
```

### 出站依赖
- **Search Provider Port**：调用搜索引擎或知识库 API
- **Export Port**：生成 Markdown / JSON / PDF 文件

---

## 跨上下文事件流

```
RecordingStopped Event
        │
        ▼
TranscriptionContext（触发转写）
        │
        ▼
TranscriptionCompleted Event
        │
        ▼
AnalysisContext（触发分析）
        │
        ▼
AnalysisCompleted Event
        │
        ▼
SolutionContext（触发搜索）
        │
        ▼
ReportGenerated Event
```

---

## 上下文映射

| 上游（提供） | 下游（消费） | 映射方式 |
|------------|------------|---------|
| RecordingContext | TranscriptionContext | 共享 RecordingId，通过事件异步触发 |
| TranscriptionContext | AnalysisContext | 共享 TranscriptionId，通过事件异步触发 |
| AnalysisContext | SolutionContext | 共享 AnalysisId + Questions，通过事件异步触发 |
| SolutionContext | — | 末端上下文，无下游 |

> 上下文之间通过 **领域事件（Domain Events）** 异步通信，保持松耦合。
