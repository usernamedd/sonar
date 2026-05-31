package com.sonar.domain.ports

import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Domain port for audio recording operations. */
interface AudioRecorderPort {
    suspend fun startRecording(): Result<String>
    suspend fun stopRecording(): AudioRecordingResult
    fun isRecording(): Boolean
}

/** Domain port for speech-to-text transcription. */
interface SttPort {
    suspend fun transcribe(audioFilePath: String, apiKey: String?): Result<String>
}

/** Domain port for LLM-based transcript analysis. */
interface LlmPort {
    suspend fun analyze(transcript: String, apiKey: String?): Result<AnalysisResultDomain>
}

/** Domain port for solution search. */
interface SearchPort {
    suspend fun search(problem: String, background: String, apiKey: String?): Result<List<SolutionDomain>>
}

/** Result of a completed audio recording. */
data class AudioRecordingResult(
    val filePath: String,
    val duration: Long,
    val format: String,
    val sampleRate: Int
)

/** Domain model for analysis result (mirrors domain entity). */
data class AnalysisResultDomain(
    val coreQuestion: String,
    val background: String,
    val solutions: List<SolutionDomain>,
    val rawLLMResponse: String? = null
)

/** Domain model for a solution. */
data class SolutionDomain(
    val title: String,
    val summary: String,
    val url: String? = null
)