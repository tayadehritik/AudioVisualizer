package com.example.audiovisualizer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.audiovisualizer.data.database.dao.VisualizerSettingsDao
import com.example.audiovisualizer.data.database.entities.VisualizerSettings

/**
 * The Room database for AudioVisualizer app.
 */
@Database(
    entities = [VisualizerSettings::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun visualizerSettingsDao(): VisualizerSettingsDao
    
    companion object {
        const val DATABASE_NAME = "audio_visualizer_database"
    }
}