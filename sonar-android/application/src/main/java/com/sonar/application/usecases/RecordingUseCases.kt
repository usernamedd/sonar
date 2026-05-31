package com.sonar.application.usecases

import com.sonar.application.ports.inbound.*
import com.sonar.data.audio.AudioRecorder
import com.sonar.data.audio.RecordingResult
import com.sonar.data.remote.llm.GeminiLlmAdapter
import com.sonar.data.remote.search.SolutionSearchAdapter
import com.sonar.data.remote.stt.WhisperSttAdapter
import com.sonar.domain.entities.Recording
import com.sonar.domain.entities.RecordingStatus
import com.sonar.domain.repositories.RecordingRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

// ─── Recording Use Cases ───────────────────────────────────────────────────────

class StartRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
    private val audioRecorder: AudioRecorder
) {
    suspend operator fun invoke(): Result<Recording> {
        return audioRecorder.startRecording().map { filePath ->
            val recording = Recording(
                status = RecordingStatus.RECORDING,
                filePath = filePath
            )
            repository.save(recording)
            recording
        }
    }
}

class StopRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
    private val audioRecorder: AudioRecorder
) {
    suspend operator fun invoke(recordingId: UUID): Result<Recording> {
        val recording = repository.findById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found"))

        return audioRecorder.stopRecording().map { result: RecordingResult ->
            val updated = recording.copy(
                status = RecordingStatus.STOPPED,
                duration = result.duration
            )
            repository.save(updated)
            updated
        }
    }
}

class GetRecordingsUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(): List<Recording> = repository.findAll()

    fun observe(): Flow<List<Recording>> = repository.observeAll()
}

class DeleteRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recordingId: UUID) {
        repository.delete(recordingId)
    }
}

// ─── Transcription Use Case ───────────────────────────────────────────────────

class TranscribeRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
    private val sttAdapter: WhisperSttAdapter,
    private val apiKey: String?
) {
    suspend operator fun invoke(recordingId: UUID): Result<Recording> {
        val recording = repository.findById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found"))

        val filePath = recording.filePath
            ?: return Result.failure(IllegalStateException("No file path for recording"))

        // Update status to transcribing
        val transcribing = recording.copy(status = RecordingStatus.TRANSCRIBING)
        repository.save(transcribing)

        return sttAdapter.transcribe(filePath, apiKey).map { transcript ->
            val updated = recording.copy(
                status = RecordingStatus.TRANSCRIBED,
                transcript = transcript
            )
            repository.save(updated)
            updated
        }
    }
}

// ─── Analysis Use Case ───────────────────────────────────────────────────────

class AnalyzeTranscriptUseCase @Inject constructor(
    private val repository: RecordingRepository,
    private val llmAdapter: GeminiLlmAdapter,
    private val searchAdapter: SolutionSearchAdapter,
    private val llmApiKey: String?,
    private val searchApiKey: String?
) {
    suspend operator fun invoke(recordingId: UUID): Result<Recording> {
        val recording = repository.findById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found"))

        val transcript = recording.transcript
            ?: return Result.failure(IllegalStateException("No transcript available"))

        // Step 1: LLM extracts core question + background
        val analyzing = recording.copy(status = RecordingStatus.ANALYZING)
        repository.save(analyzing)

        val analysisResult = llmAdapter.analyze(transcript, llmApiKey)
            .getOrElse { return Result.failure(it) }

        // Step 2: Search for solutions
        val searching = recording.copy(
            status = RecordingStatus.SEARCHING,
            analysisResult = analysisResult
        )
        repository.save(searching)

        val solutions = searchAdapter.search(
            problem = analysisResult.coreQuestion,
            background = analysisResult.background,
            apiKey = searchApiKey
        ).getOrElse { analysisResult.solutions }

        val finalResult = analysisResult.copy(solutions = solutions)
        val completed = recording.copy(
            status = RecordingStatus.COMPLETED,
            transcript = transcript,
            analysisResult = finalResult
        )
        repository.save(completed)
        return Result.success(completed)
    }
}

// ─── Full Pipeline Use Case ──────────────────────────────────────────────────

class AnalyzeRecordingUseCase @Inject constructor(
    private val transcribeUseCase: TranscribeRecordingUseCase,
    private val analyzeUseCase: AnalyzeTranscriptUseCase
) {
    suspend operator fun invoke(recordingId: UUID): Result<Recording> {
        // Step 1: Transcribe
        val transcribed = transcribeUseCase(recordingId).getOrElse {
            return Result.failure(it)
        }

        // Step 2: Analyze
        return analyzeUseCase(recordingId)
    }
}