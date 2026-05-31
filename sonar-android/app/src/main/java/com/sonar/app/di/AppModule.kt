package com.sonar.app.di

import android.content.Context
import androidx.room.Room
import com.sonar.data.audio.AudioPlayer
import com.sonar.data.audio.AudioRecorder
import com.sonar.data.local.SonarDatabase
import com.sonar.data.local.dao.RecordingDao
import com.sonar.data.remote.llm.GeminiLlmAdapter
import com.sonar.data.remote.search.SolutionSearchAdapter
import com.sonar.data.remote.stt.WhisperSttAdapter
import com.sonar.data.repository.RecordingRepositoryImpl
import com.sonar.domain.repositories.RecordingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SonarDatabase {
        return Room.databaseBuilder(
            context,
            SonarDatabase::class.java,
            "sonar.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRecordingDao(database: SonarDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    @Singleton
    fun provideRecordingRepository(dao: RecordingDao): RecordingRepository {
        return RecordingRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AudioRecorder(context)
    }

    @Provides
    @Singleton
    fun provideAudioPlayer(@ApplicationContext context: Context): AudioPlayer {
        return AudioPlayer(context)
    }

    @Provides
    @Singleton
    fun provideWhisperSttAdapter(httpClient: HttpClient): WhisperSttAdapter {
        return WhisperSttAdapter(httpClient)
    }

    @Provides
    @Singleton
    fun provideGeminiLlmAdapter(httpClient: HttpClient): GeminiLlmAdapter {
        return GeminiLlmAdapter(httpClient)
    }

    @Provides
    @Singleton
    fun provideSolutionSearchAdapter(httpClient: HttpClient): SolutionSearchAdapter {
        return SolutionSearchAdapter(httpClient)
    }
}