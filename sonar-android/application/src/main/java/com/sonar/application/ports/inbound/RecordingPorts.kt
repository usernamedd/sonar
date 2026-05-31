package com.sonar.application.ports.inbound

import kotlinx.coroutines.flow.Flow
import java.util.UUID

// Recording management ports
interface StartRecordingPort { suspend fun start(): Result<out Any> }
interface StopRecordingPort { suspend fun stop(recordingId: UUID): Result<out Any> }
interface GetRecordingsPort { suspend fun getAll(): List<out Any>; fun observe(): Flow<out List<Any>> }
interface DeleteRecordingPort { suspend fun delete(recordingId: UUID) }

// Transcription port
interface TranscribeRecordingPort { suspend fun transcribe(recordingId: UUID): Result<out Any> }

// Analysis port
interface AnalyzeRecordingPort { suspend fun analyze(recordingId: UUID): Result<out Any> }