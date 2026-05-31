package com.sonar.data.repository

import com.sonar.data.local.dao.RecordingDao
import com.sonar.data.local.entity.RecordingEntity
import com.sonar.domain.entities.Recording
import com.sonar.domain.repositories.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val dao: RecordingDao
) : RecordingRepository {

    override suspend fun save(recording: Recording) {
        dao.insert(RecordingEntity.fromDomain(recording))
    }

    override suspend fun findById(id: UUID): Recording? {
        return dao.findById(id.toString())?.toDomain()
    }

    override suspend fun findAll(): List<Recording> {
        return dao.findAll().map { it.toDomain() }
    }

    override fun observeAll(): Flow<List<Recording>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun delete(id: UUID) {
        dao.deleteById(id.toString())
    }
}