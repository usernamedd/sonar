package com.sonar.domain.events

import java.util.UUID

sealed class RecordingEvent {
    abstract val recordingId: UUID
    abstract val occurredAt: Long

    data class Started(
        override val recordingId: UUID,
        override val occurredAt: Long = System.currentTimeMillis()
    ) : RecordingEvent()

    data class Paused(
        override val recordingId: UUID,
        override val occurredAt: Long = System.currentTimeMillis()
    ) : RecordingEvent()

    data class Resumed(
        override val recordingId: UUID,
        override val occurredAt: Long = System.currentTimeMillis()
    ) : RecordingEvent()

    data class Stopped(
        override val recordingId: UUID,
        val filePath: String,
        val duration: Long,
        override val occurredAt: Long = System.currentTimeMillis()
    ) : RecordingEvent()
}
