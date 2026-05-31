package com.sonar.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.sonar.domain.ports.AudioRecorderPort
import com.sonar.domain.ports.AudioRecordingResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioRecorderPort {

    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false
    private var startTime: Long = 0L
    private var currentFormat: String = "AAC"
    private var currentSampleRate: Int = 44100

    override suspend fun startRecording(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val fileName = "${UUID.randomUUID()}.${currentFormat.lowercase()}"
            val file = File(recordingsDir, fileName)
            currentFilePath = file.absolutePath

            recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(getOutputFormat(currentFormat))
                setAudioEncoder(getAudioEncoder(currentFormat))
                setAudioSamplingRate(currentSampleRate)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            startTime = System.currentTimeMillis()
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            Result.failure(e)
        }
    }

    override suspend fun stopRecording(): AudioRecordingResult {
        val duration = System.currentTimeMillis() - startTime
        val filePath = currentFilePath ?: throw IllegalStateException("No recording in progress")

        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        isRecording = false
        currentFilePath = null

        return AudioRecordingResult(
            filePath = filePath,
            duration = duration,
            format = currentFormat,
            sampleRate = currentSampleRate
        )
    }

    override fun isRecording(): Boolean = isRecording

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun getOutputFormat(format: String): Int = when (format.uppercase()) {
        "AAC" -> MediaRecorder.OutputFormat.AAC_ADTS
        "WAV" -> MediaRecorder.OutputFormat.DEFAULT
        "MP3" -> MediaRecorder.OutputFormat.MPEG_4
        else -> MediaRecorder.OutputFormat.AAC_ADTS
    }

    private fun getAudioEncoder(format: String): Int = when (format.uppercase()) {
        "AAC" -> MediaRecorder.AudioEncoder.AAC
        "WAV" -> MediaRecorder.AudioEncoder.DEFAULT
        "MP3" -> MediaRecorder.AudioEncoder.AAC
        else -> MediaRecorder.AudioEncoder.AAC
    }
}
