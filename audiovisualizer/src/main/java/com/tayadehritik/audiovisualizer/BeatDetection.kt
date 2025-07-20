package com.tayadehritik.audiovisualizer

import kotlinx.coroutines.flow.StateFlow

enum class FrequencyBand(val displayName: String, val minHz: Int, val maxHz: Int) {
    ALL_FREQUENCIES("All Frequencies", 20, 20000),
    SUB_BASS("Sub Bass (20-60Hz)", 20, 60),
    BASS("Bass (60-250Hz)", 60, 250),
    LOW_MIDS("Low Mids (250-500Hz)", 250, 500),
    MIDS("Mids (500-2kHz)", 500, 2000),
    HIGH_MIDS("High Mids (2-4kHz)", 2000, 4000),
    TREBLE("Treble (4-20kHz)", 4000, 20000)
}

data class BeatDetectionConfig(
    val sensitivity: Float = 0.7f,
    val ignoreTimeMs: Long = 100L,
    val peakPersistenceMs: Long = 200L,
    val historyWindowMs: Long = 1500L,
    val smoothingFactor: Float = 0.8f,
    val frequencyBand: FrequencyBand = FrequencyBand.ALL_FREQUENCIES
)

data class BeatEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val intensity: Float = 0f,
    val frequency: Float = 0f
)

data class BeatDetectionState(
    val isEnabled: Boolean = false,
    val currentEnergy: Float = 0f,
    val averageEnergy: Float = 0f,
    val energyVariance: Float = 0f,
    val lastBeatTimestamp: Long = 0L,
    val beatIntensity: Float = 0f
)

internal class EnergyHistory(private val maxSizeMs: Long) {
    private data class EnergySample(val timestamp: Long, val energy: Float)
    private val samples = mutableListOf<EnergySample>()
    
    fun addSample(energy: Float) {
        val now = System.currentTimeMillis()
        samples.add(EnergySample(now, energy))
        
        // Remove old samples
        val cutoffTime = now - maxSizeMs
        samples.removeAll { it.timestamp < cutoffTime }
    }
    
    fun getAverage(): Float {
        if (samples.isEmpty()) return 0f
        return samples.map { it.energy }.average().toFloat()
    }
    
    fun getVariance(): Float {
        if (samples.size < 2) return 0f
        val avg = getAverage()
        return samples.map { (it.energy - avg) * (it.energy - avg) }.average().toFloat()
    }
    
    fun clear() {
        samples.clear()
    }
}