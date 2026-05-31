package com.sonar.application.usecases

import com.sonar.domain.entities.Recording
import com.sonar.domain.entities.RecordingStatus
import com.sonar.domain.events.RecordingEvent
import com.sonar.domain.repositories.RecordingRepository
import java.util.UUID

class StartRecordingUseCase(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(): Recording {
        val recording = Recording(status = RecordingStatus.RECORDING)
        repository.save(recording)
        return recording
    }
}

class StopRecordingUseCase(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recordingId: UUID): Recording {
        val recording = repository.findById(recordingId)
            ?: throw IllegalArgumentException("Recording not found: $recordingId")

        val stopped = recording.copy(
            status = RecordingStatus.STOPPED,
            filePath = generateFilePath(recording.id),
            duration = System.currentTimeMillis() - recording.createdAt
        )
        repository.save(stopped)
        return stopped
    }

    private fun generateFilePath(id: UUID) = "/data/recordings/$id.aac"
}

class GetRecordingsUseCase(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(): List<Recording> = repository.findAll()
}

class DeleteRecordingUseCase(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recordingId: UUID) {
        repository.delete(recordingId)
    }
}
