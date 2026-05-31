package com.sonar.app.di

import android.content.Context
import androidx.room.Room
import com.sonar.data.llm.GeminiLlmAdapter
import com.sonar.data.local.SonarDatabase
import com.sonar.data.local.dao.RecordingDao
import com.sonar.data.repository.RecordingRepositoryImpl
import com.sonar.data.search.SolutionSearchAdapter
import com.sonar.data.stt.WhisperSttAdapter
import com.sonar.data.audio.AudioRecorderAdapter
import com.sonar.domain.GeminiApiKey
import com.sonar.domain.SearchApiKey
import com.sonar.domain.WhisperApiKey
import com.sonar.domain.ports.AudioRecorderPort
import com.sonar.domain.ports.LlmPort
import com.sonar.domain.ports.SearchPort
import com.sonar.domain.ports.SttPort
import com.sonar.domain.repositories.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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
    @WhisperApiKey
    fun provideWhisperApiKey(): String = System.getenv("WHISPER_API_KEY") ?: ""

    @Provides
    @Singleton
    @GeminiApiKey
    fun provideGeminiApiKey(): String = System.getenv("GEMINI_API_KEY") ?: ""

    @Provides
    @Singleton
    @SearchApiKey
    fun provideSearchApiKey(): String = System.getenv("SEARCH_API_KEY") ?: ""
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdapterModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindAudioRecorderPort(impl: AudioRecorderAdapter): AudioRecorderPort

    @Binds
    @Singleton
    abstract fun bindSttPort(impl: WhisperSttAdapter): SttPort

    @Binds
    @Singleton
    abstract fun bindLlmPort(impl: GeminiLlmAdapter): LlmPort

    @Binds
    @Singleton
    abstract fun bindSearchPort(impl: SolutionSearchAdapter): SearchPort
}
