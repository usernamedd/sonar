package com.sonar.data.remote.llm

import com.sonar.domain.entities.AnalysisResult
import com.sonar.domain.entities.Solution
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GeminiRequest(
    val contents: List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

/**
 * LLM Adapter using Google Gemini API.
 * Extracts core question + background from transcript.
 */
@Singleton
class GeminiLlmAdapter @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Analyze transcript to extract core question and background.
     * If API key not set, returns mock result for development.
     */
    suspend fun analyze(transcript: String, apiKey: String?): Result<AnalysisResult> {
        return if (apiKey.isNullOrBlank()) {
            // Development mode: return mock analysis
            Result.success(MOCK_ANALYSIS_RESULT)
        } else {
            try {
                val prompt = buildAnalysisPrompt(transcript)
                val response = httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent") {
                    parameter("key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt))))))
                }

                if (response.status.isSuccess()) {
                    val json = Json { ignoreUnknownKeys = true }
                    val body = response.bodyAsText()
                    val parsed = json.decodeFromString<GeminiResponse>(body)
                    val text = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: throw Exception("No response from Gemini")
                    Result.success(parseAnalysisResult(text))
                } else {
                    Result.failure(Exception("Gemini API error: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildAnalysisPrompt(transcript: String): String = """
你是一个对话分析助手。请分析以下对话内容，提取出：
1. 一个核心问题（对话中讨论的关键问题，仅提取一个，具体明确）
2. 这个问题产生的背景上下文（1-3句话）
3. 解决方案（1-2条可操作的具体建议）

请用JSON格式返回：
{
  "coreQuestion": "问题描述",
  "background": "背景描述",
  "solutions": [
    {"title": "方案标题", "summary": "方案说明", "url": "参考链接（可选）"}
  ]

对话内容：
$transcript
    """.trimIndent()

    private fun parseAnalysisResult(text: String): AnalysisResult {
        // Try to parse as JSON
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val parsed = json.decodeFromString<AnalysisResultJson>(text)
            AnalysisResult(
                coreQuestion = parsed.coreQuestion,
                background = parsed.background,
                solutions = parsed.solutions.map { Solution(it.title, it.summary, it.url) },
                rawLLMResponse = text
            )
        } catch (e: Exception) {
            // Fallback: parse as plain text
            parsePlainTextResult(text)
        }
    }

    private fun parsePlainTextResult(text: String): AnalysisResult {
        val lines = text.lines().filter { it.isNotBlank() }
        return AnalysisResult(
            coreQuestion = lines.getOrNull(0) ?: "无法提取核心问题",
            background = lines.drop(1).take(3).joinToString("\n"),
            solutions = emptyList(),
            rawLLMResponse = text
        )
    }

    companion object {
        private val MOCK_ANALYSIS_RESULT = AnalysisResult(
            coreQuestion = "新产品发布计划的时间节点和分阶段发布方案未能确定",
            background = "在产品发布会议中，张总和李总讨论新产品的发布时间节点。李总询问能否月底前完成，张总表示技术开发还需要两周时间。会上还涉及预算可能需要追加20%的问题，但具体细节尚未落实。",
            solutions = listOf(
                Solution(
                    title = "采用分阶段发布策略",
                    summary = "先发布核心功能，后续功能分批迭代上线，降低一次性发布的技术风险",
                    url = null
                ),
                Solution(
                    title = "提前与技术团队对齐方案",
                    summary = "安排与研发团队的技术方案评审会议，确认分阶段发布的可行性和具体时间表",
                    url = null
                )
            )
        )
    }
}

@Serializable
data class AnalysisResultJson(
    val coreQuestion: String,
    val background: String,
    val solutions: List<SolutionJson>
)

@Serializable
data class SolutionJson(
    val title: String,
    val summary: String,
    val url: String? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)