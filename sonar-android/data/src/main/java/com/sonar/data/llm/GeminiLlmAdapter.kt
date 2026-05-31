package com.sonar.data.llm

import com.sonar.domain.entities.AnalysisResult
import com.sonar.domain.entities.Solution
import com.sonar.domain.ports.AnalysisResultDomain
import com.sonar.domain.ports.LlmPort
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiLlmAdapter @Inject constructor() : LlmPort {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun analyze(transcript: String, apiKey: String?): Result<AnalysisResultDomain> {
        return if (apiKey.isNullOrBlank()) {
            Result.success(MOCK_ANALYSIS_RESULT)
        } else {
            try {
                val prompt = buildAnalysisPrompt(transcript)
                val jsonBody = """
                    {"contents":[{"parts":[{"text":"$prompt"}]}]}
                """.trimIndent()

                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val text = extractTextFromGeminiResponse(body)
                        Result.success(parseAnalysisResult(text))
                    } else {
                        Result.failure(Exception("Gemini API error: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildAnalysisPrompt(transcript: String): String = """
你是一个对话分析助手。请分析以下对话内容，提取：
1. 一个核心问题（仅提取一个，具体明确）
2. 问题产生的背景上下文（1-3句话）
3. 1-2条可操作的具体建议

请用JSON格式返回：
{
  "coreQuestion": "问题描述",
  "background": "背景描述",
  "solutions": [{"title": "方案标题", "summary": "方案说明", "url": "参考链接（可选）"}]
}
对话内容：
$transcript
    """.trimIndent()

    private fun extractTextFromGeminiResponse(json: String): String {
        val regex = """"text"\s*:\s*"(.*?)"(?:\s*,|\s*})""".toRegex(RegexOption.DOT_MATCHES_ALL)
        return regex.find(json)?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: throw Exception("Failed to parse Gemini response")
    }

    private fun parseAnalysisResult(text: String): AnalysisResultDomain {
        return try {
            val coreQ = """coreQuestion["\s:]+([^",\n]+)""".toRegex().find(text)?.groupValues?.get(1) ?: "无法提取核心问题"
            val background = """background["\s:]+([^",\n]+)""".toRegex().find(text)?.groupValues?.get(1) ?: ""
            AnalysisResultDomain(
                coreQuestion = coreQ,
                background = background,
                solutions = emptyList(),
                rawLLMResponse = text
            )
        } catch (e: Exception) {
            AnalysisResultDomain(
                coreQuestion = text.lines().firstOrNull() ?: "无法提取核心问题",
                background = text.lines().drop(1).take(3).joinToString("\n"),
                solutions = emptyList(),
                rawLLMResponse = text
            )
        }
    }

    companion object {
        private val MOCK_ANALYSIS_RESULT = AnalysisResultDomain(
            coreQuestion = "新产品发布计划的时间节点和分阶段发布方案未能确定",
            background = "在产品发布会议中，张总和李总讨论新产品的发布时间节点。李总询问能否月底前完成，张总表示技术开发还需要两周时间。会上还涉及预算可能需要追加20%的问题，但具体细节尚未落实。",
            solutions = listOf(
                com.sonar.domain.ports.SolutionDomain(
                    title = "采用分阶段发布策略",
                    summary = "先发布核心功能，后续功能分批迭代上线，降低一次性发布的技术风险",
                    url = null
                ),
                com.sonar.domain.ports.SolutionDomain(
                    title = "提前与技术团队对齐方案",
                    summary = "安排与研发团队的技术方案评审会议，确认分阶段发布的可行性和具体时间表",
                    url = null
                )
            )
        )
    }
}
