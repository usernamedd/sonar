package com.sonar.data.remote.stt

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WhisperResponse(
    val text: String,
    val segments: List<WhisperSegment>? = null
)

@Serializable
data class WhisperSegment(
    val start: Double,
    val end: Double,
    val text: String
)

/**
 * STT Adapter using OpenAI Whisper API.
 * Falls back to local file path if API key not configured.
 */
@Singleton
class WhisperSttAdapter @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Transcribe audio file to text.
     * If WHISPER_API_KEY is not set, returns a mock result for development.
     */
    suspend fun transcribe(audioFilePath: String, apiKey: String?): Result<String> {
        return if (apiKey.isNullOrBlank()) {
            // Development mode: return mock transcription
            Result.success(MOCK_TRANSCRIPTION)
        } else {
            try {
                val response = httpClient.submitFormWithBinaryData(
                    url = "https://api.openai.com/v1/audio/transcriptions",
                    formData = formData {
                        append("file", File(audioFilePath).readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "audio/aac")
                            append(HttpHeaders.ContentDisposition, "filename=\"recording.aac\"")
                        })
                        append("model", "whisper-1")
                        append("response_format", "verbose_json")
                    }
                ) {
                    headers { append(HttpHeaders.Authorization, "Bearer $apiKey") }
                }

                if (response.status.isSuccess()) {
                    val result = response.body<WhisperResponse>()
                    Result.success(result.text)
                } else {
                    Result.failure(Exception("Whisper API error: ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    companion object {
        private const val MOCK_TRANSCRIPTION = """
            张总：我们今天的会议主要讨论一下新产品的发布计划。
            李总：好的，我这边有几个问题想确认一下。
            张总：请说。
            李总：首先是发布时间节点，能否在月底之前完成？
            张总：这个有点紧张，我看了一下开发进度，至少还需要两周。
            李总：那如果分阶段发布呢？先上核心功能。
            张总：这样可行，但需要和研发确认一下技术方案。
            李总：还有一个问题是关于市场推广的预算。
            张总：预算方面我们可能需要再追加一些。
            李总：追加多少？
            张总：大概还需要增加20%的预算。
        """.trimIndent()
    }
}