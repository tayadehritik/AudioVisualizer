package com.example.audiovisualizer.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing visualizer settings stored in the database.
 */
@Entity(tableName = "visualizer_settings")
data class VisualizerSettings(
    @PrimaryKey
    val id: Int = 1, // Single row for app settings
    val barCount: Int = 32,
    val primaryColor: String = "#2196F3", // Material Blue
    val backgroundColor: String = "#000000", // Black
    val animationSpeed: Int = 100, // milliseconds
    val sensitivity: Float = 1.0f,
    val smoothingEnabled: Boolean = true,
    val beatDetectionEnabled: Boolean = false,
    val beatSensitivity: Float = 0.7f,
    val beatSmoothingFactor: Float = 0.8f,
    val beatFrequencyBand: String = "ALL_FREQUENCIES",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefault() = VisualizerSettings()
    }
}