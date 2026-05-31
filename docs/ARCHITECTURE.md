# Sonar 技术架构文档

## 1. 技术选型

### 移动端

| 平台 | 编程语言 | 技术框架 | 说明 |
|------|---------|---------|------|
| Android | Kotlin | 原生开发（Jetpack Compose） | 优先使用 Jetpack Compose，View 仅作兼容 |
| iOS | Swift | 原生开发（SwiftUI） | 优先使用 SwiftUI，UIKit 仅作兼容 |

> 桌面端暂不考虑。

### 服务端

| 组件 | 技术选型 |
|------|---------|
| 运行时 | Node.js (bun) |
| 语言 | TypeScript |
| 框架 | Fastify |
| 数据库 | PostgreSQL |
| AI | OpenAI GPT-4o-mini |

---

## 2. 架构设计

### 2.1 六边形架构（Hexagonal Architecture）

```
                    ┌─────────────────────────────────┐
                    │         Driving Adapters        │
                    │  (API Controllers, CLI, UI)     │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────▼──────────────────┐
                    │            Ports                │
                    │   (Inbound Interfaces/API)      │
                    └──────────────┬──────────────────┘
                                   │
        ┌───────────────────────────▼───────────────────────────┐
        │                    Application                        │
        │                 (Use Cases / Services)                │
        │                                                         │
        │  ┌─────────────────────────────────────────────────┐  │
        │  │                   Domain                          │  │
        │  │         (Entities, Value Objects,                │  │
        │  │          Domain Services, Events)                 │  │
        │  └─────────────────────────────────────────────────┘  │
        │                                                         │
        │                 (Repositories - Interfaces)            │
        └───────────────────────────┬───────────────────────────┘
                                    │
                    ┌──────────────▼──────────────────┐
                    │            Ports                │
                    │  (Outbound Interfaces/Ports)    │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────▼──────────────────┐
                    │       Driven Adapters           │
                    │  (DB, AI Provider, File Storage)│
                    └─────────────────────────────────┘
```

### 2.2 DDD 分层（Android / iOS）

```
app/
├── domain/                    # 领域层（最核心，零依赖）
│   ├── entities/              # 聚合根、实体
│   ├── value-objects/         # 值对象
│   ├── services/              # 领域服务
│   ├── events/                # 领域事件
│   └── repositories/          # 仓储接口（抽象，不实现）
│
├── application/               # 应用层（编排用例）
│   ├── usecases/              # 用例
│   ├── dto/                   # 数据传输对象
│   └── ports/                 # 端口定义
│       ├── inbound/           # 入站端口（提供给 UI 层调用）
│       └── outbound/          # 出站端口（对外部服务的抽象）
│
├── adapter/                   # 适配器层
│   ├── primary/               # 主适配器（UI 适配器）
│   │   ├── android/           # Android ViewModel / Composable
│   │   └── ios/               # iOS ViewModel / SwiftUI View
│   └── secondary/             # 从适配器（外部服务实现）
│       ├── api/               # HTTP 客户端
│       ├── storage/           # 本地存储
│       └── ai/                # AI 服务调用
│
└── infrastructure/            # 基础设施层
    ├── di/                    # 依赖注入配置
    └── platform/              # 平台特定实现
```

### 2.3 Android 模块划分

```
app/
├── domain/                    # :domain 模块（纯 Kotlin，无 Android 依赖）
├── application/              # :application 模块（依赖 :domain）
└── adapter/
    ├── primary/
    │   └── androidApp/        # :androidApp 模块（Application + UI）
    └── secondary/
        └── androidData/      # :androidData 模块（Repository 实现）
```

### 2.4 iOS 模块划分

```
ios/
├── Sonar/
│   ├── Domain/                # 纯 Swift 域，无依赖
│   ├── Application/           # 用例层
│   └── Adapters/
│       ├── Primary/          # SwiftUI Views + ViewModels
│       └── Secondary/         # Repository 实现、网络客户端
└── SonarTests/
```

---

## 3. 核心领域概念

### 3.1 聚合

- **Recording（录音）**：聚合根，管理一段录音的完整生命周期
- **Transcription（转写）**：值对象，包含原始文本和分段信息
- **Analysis（分析结果）**：值对象，包含提取的问题和背景
- **Solution（解决方案）**：实体，属于分析结果的子实体

### 3.2 领域服务

- **TranscriptionService**：录音转文字
- **AnalysisService**：大模型分析（提取问题 + 背景）
- **SearchService**：搜索解决方案

### 3.3 用例

| 用例 | 输入 | 输出 |
|------|------|------|
| StartRecording | - | Recording |
| StopRecording | RecordingId | Recording（带文件路径） |
| TranscribeAudio | RecordingId | Transcription |
| AnalyzeContent | Transcription | Analysis |
| SearchSolutions | Analysis.questions | List[Solution] |
| ExportReport | Analysis + Solutions | Report |

---

## 4. 技术债务与决策记录

| 日期 | 决策 | 原因 |
|------|------|------|
| 2026-05-31 | 移动端只做 Android + iOS，不做桌面端 | 资源聚焦，快速验证 |
| 2026-05-31 | Android 优先 Jetpack Compose，iOS 优先 SwiftUI | 现代化 UI 框架 |
| 2026-05-31 | Android 按模块（domain/application/data）划分，iOS 按功能划分 | 各自平台习惯 |
| 2026-05-31 | 服务端用 Node.js (bun) + Fastify + TypeScript | 轻量、高性能、统一语言 |
| 2026-05-31 | AI 模型用 GPT-4o-mini | 成本与效果平衡 |
