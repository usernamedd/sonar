package com.sonar.data.stt

import com.sonar.domain.ports.SttPort
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperSttAdapter @Inject constructor() : SttPort {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun transcribe(audioFilePath: String, apiKey: String?): Result<String> {
        return if (apiKey.isNullOrBlank()) {
            Result.success(MOCK_TRANSCRIPTION)
        } else {
            try {
                val file = File(audioFilePath)
                val requestBody = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.readBytes().toRequestBody("audio/aac".toMediaType())
                    )
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("response_format", "verbose_json")
                    .build()

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val text = extractTextFromWhisperResponse(body)
                        Result.success(text)
                    } else {
                        Result.failure(Exception("Whisper API error: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun extractTextFromWhisperResponse(json: String): String {
        // Simple regex extraction for {"text": "..."}
        val regex = """"text"\s*:\s*"(.*?)"(?:\s*,|\s*})""".toRegex(RegexOption.DOT_MATCHES_ALL)
        return regex.find(json)?.groupValues?.get(1)?.replace("\\n", "\n") ?: MOCK_TRANSCRIPTION
    }

    companion object {
        private val MOCK_TRANSCRIPTION = """
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
