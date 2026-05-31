package com.sonar.domain.repositories

import com.sonar.domain.entities.Recording
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface RecordingRepository {
    suspend fun save(recording: Recording)
    suspend fun findById(id: UUID): Recording?
    suspend fun findAll(): List<Recording>
    fun observeAll(): Flow<List<Recording>>
    suspend fun delete(id: UUID)
}
