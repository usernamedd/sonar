package com.sonar.application.usecases

import com.sonar.domain.GeminiApiKey
import com.sonar.domain.SearchApiKey
import com.sonar.domain.WhisperApiKey
import com.sonar.domain.entities.Recording
import com.sonar.domain.entities.RecordingStatus
import com.sonar.domain.entities.Solution
import com.sonar.domain.ports.*
import com.sonar.domain.repositories.RecordingRepository
import java.util.UUID
import javax.inject.Inject

// ─── Recording Use Cases ───────────────────────────────────────────────────────

class StartRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
    private val audioRecorder: AudioRecorderPort
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
    private val audioRecorder: AudioRecorderPort
) {
    suspend operator fun invoke(recordingId: UUID): Result<Recording> {
        val recording = repository.findById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found"))

        val result = audioRecorder.stopRecording()
        val updated = recording.copy(
            status = RecordingStatus.STOPPED,
            duration = result.duration
        )
        repository.save(updated)
        return Result.success(updated)
    }
}

class GetRecordingsUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(): List<Recording> = repository.findAll()
    fun observe(): kotlinx.coroutines.flow.Flow<List<Recording>> = repository.observeAll()
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
    private val sttPort: SttPort,
    @WhisperApiKey private val apiKey: String
) {
    suspend operator fun invoke(recordingId: UUID): Result<Recording> {
        val recording = repository.findById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found"))

        val filePath = recording.filePath
            ?: return Result.failure(IllegalStateException("No file path for recording"))

        val transcribing = recording.copy(status = RecordingStatus.TRANSCRIBING)
        repository.save(transcribing)

        return sttPort.transcribe(filePath, apiKey).map { transcript ->
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
    private val llmPort: LlmPort,
    private val searchPort: SearchPort,
    @GeminiApiKey private val llmApiKey: String,
    @SearchApiKey private val searchApiKey: String
) {
    suspend operator fun invoke(recordingId: UUID): Result<Recording> {
        val recording = repository.findById(recordingId)
            ?: return Result.failure(IllegalArgumentException("Recording not found"))

        val transcript = recording.transcript
            ?: return Result.failure(IllegalStateException("No transcript available"))

        // Step 1: LLM extracts core question + background
        val analyzing = recording.copy(status = RecordingStatus.ANALYZING)
        repository.save(analyzing)

        val domainResult = llmPort.analyze(transcript, llmApiKey)
            .getOrElse { return Result.failure(it) }

        // Map domain model to entity model
        val entityResult = com.sonar.domain.entities.AnalysisResult(
            coreQuestion = domainResult.coreQuestion,
            background = domainResult.background,
            solutions = domainResult.solutions.map {
                com.sonar.domain.entities.Solution(it.title, it.summary, it.url)
            },
            rawLLMResponse = domainResult.rawLLMResponse
        )

        // Step 2: Search for solutions
        val searching = recording.copy(
            status = RecordingStatus.SEARCHING,
            analysisResult = entityResult
        )
        repository.save(searching)

        val rawSolutions: List<SolutionDomain> = searchPort.search(
            problem = entityResult.coreQuestion,
            background = entityResult.background,
            apiKey = searchApiKey
        ).getOrElse { entityResult.solutions.map { s -> SolutionDomain(s.title, s.summary, s.url) } }

        val finalResult = entityResult.copy(
            solutions = rawSolutions.map { Solution(it.title, it.summary, it.url) }
        )
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
        transcribeUseCase(recordingId).getOrElse {
            return Result.failure(it)
        }
        return analyzeUseCase(recordingId)
    }
}
