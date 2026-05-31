package com.sonar.data.audio

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPath: String? = null
    private var onCompletionListener: (() -> Unit)? = null

    suspend fun play(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stop()

            if (!File(filePath).exists()) {
                return@withContext Result.failure(IllegalArgumentException("File not found: $filePath"))
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener { onCompletionListener?.invoke() }
                prepare()
                start()
            }
            currentPath = filePath
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentPath = null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }
}