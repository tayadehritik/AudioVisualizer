package com.example.audiovisualizer.data.repository

import com.example.audiovisualizer.data.database.dao.VisualizerSettingsDao
import com.example.audiovisualizer.data.database.entities.VisualizerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
/**
 * Repository for managing visualizer settings.
 * This class abstracts the data source from the rest of the app.
 */
class SettingsRepository(
    private val settingsDao: VisualizerSettingsDao
) {
    
    /**
     * Get the current settings as a Flow.
     * If no settings exist, returns default settings.
     */
    val settings: Flow<VisualizerSettings> = settingsDao.getSettings().map { settings ->
        settings ?: VisualizerSettings.getDefault().also {
            // Insert default settings if none exist
            settingsDao.insertSettings(it)
        }
    }
    
    /**
     * Get current settings once (not as Flow).
     */
    suspend fun getSettingsOnce(): VisualizerSettings {
        return settingsDao.getSettingsOnce() ?: VisualizerSettings.getDefault().also {
            settingsDao.insertSettings(it)
        }
    }
    
    /**
     * Update bar count setting.
     */
    suspend fun updateBarCount(barCount: Int) {
        val current = getSettingsOnce()
        settingsDao.updateSettings(current.copy(
            barCount = barCount,
            lastUpdated = System.currentTimeMillis()
        ))
    }
    
    /**
     * Update primary color.
     */
    suspend fun updatePrimaryColor(color: String) {
        val current = getSettingsOnce()
        settingsDao.updateSettings(current.copy(
            primaryColor = color,
            lastUpdated = System.currentTimeMillis()
        ))
    }
    
    /**
     * Update animation speed.
     */
    suspend fun updateAnimationSpeed(speed: Int) {
        val current = getSettingsOnce()
        settingsDao.updateSettings(current.copy(
            animationSpeed = speed,
            lastUpdated = System.currentTimeMillis()
        ))
    }
    
    /**
     * Update beat detection enabled state.
     */
    suspend fun updateBeatDetectionEnabled(enabled: Boolean) {
        val current = getSettingsOnce()
        settingsDao.updateSettings(current.copy(
            beatDetectionEnabled = enabled,
            lastUpdated = System.currentTimeMillis()
        ))
    }
    
    /**
     * Update beat sensitivity.
     */
    suspend fun updateBeatSensitivity(sensitivity: Float) {
        val current = getSettingsOnce()
        settingsDao.updateSettings(current.copy(
            beatSensitivity = sensitivity,
            lastUpdated = System.currentTimeMillis()
        ))
    }
    
    /**
     * Update beat smoothing factor.
     */
    suspend fun updateBeatSmoothingFactor(factor: Float) {
        val current = getSettingsOnce()
        settingsDao.updateSettings(current.copy(
            beatSmoothingFactor = factor,
            lastUpdated = System.currentTimeMillis()
        ))
    }
    
    /**
     * Update all settings at once.
     */
    suspend fun updateSettings(settings: VisualizerSettings) {
        settingsDao.updateSettings(settings.copy(
            lastUpdated = System.currentTimeMillis()
        ))
    }
    
    /**
     * Reset settings to default.
     */
    suspend fun resetToDefault() {
        settingsDao.insertSettings(VisualizerSettings.getDefault())
    }
}