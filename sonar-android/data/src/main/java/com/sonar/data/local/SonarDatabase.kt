package com.sonar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sonar.data.local.dao.RecordingDao
import com.sonar.data.local.entity.RecordingEntity

@Database(
    entities = [RecordingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SonarDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}