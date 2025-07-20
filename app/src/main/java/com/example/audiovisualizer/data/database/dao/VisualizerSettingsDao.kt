package com.example.audiovisualizer.data.database.dao

import androidx.room.*
import com.example.audiovisualizer.data.database.entities.VisualizerSettings
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for visualizer settings.
 */
@Dao
interface VisualizerSettingsDao {
    
    @Query("SELECT * FROM visualizer_settings WHERE id = 1")
    fun getSettings(): Flow<VisualizerSettings?>
    
    @Query("SELECT * FROM visualizer_settings WHERE id = 1")
    suspend fun getSettingsOnce(): VisualizerSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: VisualizerSettings)
    
    @Update
    suspend fun updateSettings(settings: VisualizerSettings)
    
    @Query("UPDATE visualizer_settings SET barCount = :barCount WHERE id = 1")
    suspend fun updateBarCount(barCount: Int)
    
    @Query("UPDATE visualizer_settings SET primaryColor = :color WHERE id = 1")
    suspend fun updatePrimaryColor(color: String)
    
    @Query("UPDATE visualizer_settings SET animationSpeed = :speed WHERE id = 1")
    suspend fun updateAnimationSpeed(speed: Int)
    
    @Query("DELETE FROM visualizer_settings")
    suspend fun deleteAll()
}