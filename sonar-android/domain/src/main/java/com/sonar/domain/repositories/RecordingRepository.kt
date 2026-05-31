package com.sonar.domain.repositories

import com.sonar.domain.entities.Recording
import java.util.UUID

interface RecordingRepository {
    suspend fun save(recording: Recording)
    suspend fun findById(id: UUID): Recording?
    suspend fun findAll(): List<Recording>
    suspend fun delete(id: UUID)
}
