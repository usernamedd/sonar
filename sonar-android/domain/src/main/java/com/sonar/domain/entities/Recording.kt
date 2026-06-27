package com.sonar.domain.entities

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Recording 聚合根
 * 管理一段录音的完整生命周期
 */
data class Recording(
    val id: UUID = UUID.randomUUID(),
    val status: RecordingStatus = RecordingStatus.IDLE,
    val filePath: String? = null,
    val duration: Long = 0L, // milliseconds
    val sampleRate: Int = 44100,
    val format: AudioFormat = AudioFormat.AAC,
    val createdAt: Long = System.currentTimeMillis(),
    val metadata: RecordingMetadata = RecordingMetadata(),
    val transcript: String? = null,
    val analysisResult: AnalysisResult? = null
)

@Serializable
data class AnalysisResult(
    val coreQuestion: String,
    val background: String,
    val solutions: List<Solution>,
    val rawLLMResponse: String? = null
)

@Serializable
data class Solution(
    val title: String,
    val summary: String,
    val url: String? = null
)

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED,
    TRANSCRIBING,
    TRANSCRIBED,
    ANALYZING,
    ANALYZED,
    SEARCHING,
    COMPLETED,
    FAILED
}

enum class AudioFormat {
    AAC, WAV, MP3
}

data class RecordingMetadata(
    val title: String? = null,
    val speakerCount: Int? = null,
    val tags: List<String> = emptyList()
)
