package com.sonar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sonar.domain.entities.AnalysisResult
import com.sonar.domain.entities.AudioFormat
import com.sonar.domain.entities.Recording
import com.sonar.domain.entities.RecordingMetadata
import com.sonar.domain.entities.RecordingStatus
import com.sonar.domain.entities.Solution
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey
    val id: String,
    val status: String,
    val filePath: String?,
    val duration: Long,
    val sampleRate: Int,
    val format: String,
    val createdAt: Long,
    val title: String?,
    val speakerCount: Int?,
    val tags: String, // comma-separated
    val transcript: String?,
    val coreQuestion: String?,
    val background: String?,
    val solutions: String?, // JSON array of Solution
    val rawLLMResponse: String?
) {
    fun toDomain(): Recording = Recording(
        id = UUID.fromString(id),
        status = RecordingStatus.valueOf(status),
        filePath = filePath,
        duration = duration,
        sampleRate = sampleRate,
        format = AudioFormat.valueOf(format),
        createdAt = createdAt,
        metadata = RecordingMetadata(
            title = title,
            speakerCount = speakerCount,
            tags = tags.split(",").filter { it.isNotBlank() }
        ),
        transcript = transcript,
        analysisResult = if (coreQuestion != null && background != null) {
            AnalysisResult(
                coreQuestion = coreQuestion,
                background = background,
                solutions = solutions?.let { parseSolutions(it) } ?: emptyList(),
                rawLLMResponse = rawLLMResponse
            )
        } else null
    )

    private fun parseSolutions(json: String): List<Solution> {
        return try {
            Json.decodeFromString<List<Solution>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun fromDomain(recording: Recording): RecordingEntity = RecordingEntity(
            id = recording.id.toString(),
            status = recording.status.name,
            filePath = recording.filePath,
            duration = recording.duration,
            sampleRate = recording.sampleRate,
            format = recording.format.name,
            createdAt = recording.createdAt,
            title = recording.metadata.title,
            speakerCount = recording.metadata.speakerCount,
            tags = recording.metadata.tags.joinToString(","),
            transcript = recording.transcript,
            coreQuestion = recording.analysisResult?.coreQuestion,
            background = recording.analysisResult?.background,
            solutions = recording.analysisResult?.solutions?.let {
                Json.encodeToString(it)
            },
            rawLLMResponse = recording.analysisResult?.rawLLMResponse
        )
    }
}