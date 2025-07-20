package com.tayadehritik.audiovisualizer

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

/**
 * State holder for audio visualization data.
 * This class provides a safe, compose-friendly API for accessing audio FFT data.
 */
@Stable
class AudioVisualizerState internal constructor(
    private val audioVisualizer: AudioVisualizer
) {
    /**
     * Flow of FFT (Fast Fourier Transform) data from the audio stream.
     * Emits byte arrays containing frequency magnitude data.
     * The data is updated at the capture rate specified during initialization.
     */
    val fftData: StateFlow<ByteArray?> = audioVisualizer.fftDataFlow
    
    /**
     * Indicates whether the visualizer is currently active and capturing data.
     */
    val isActive: StateFlow<Boolean> = audioVisualizer.isActive
    
    /**
     * Flow of beat detection events.
     * Emits BeatEvent objects when a beat is detected (if beat detection is enabled).
     */
    val beatFlow: StateFlow<BeatEvent?> = audioVisualizer.beatFlow
    
    /**
     * Current state of the beat detection system.
     */
    val beatDetectionState: StateFlow<BeatDetectionState> = audioVisualizer.beatDetectionState
    
    /**
     * Pauses the audio visualization.
     * Call this when the UI is not visible to save battery.
     */
    fun pause() {
        audioVisualizer.stop()
    }
    
    /**
     * Resumes the audio visualization.
     * Call this when the UI becomes visible again.
     */
    fun resume() {
        audioVisualizer.start()
    }
    
    /**
     * Enables or disables beat detection.
     * When disabled, beatFlow will stop emitting events.
     */
    fun setBeatDetectionEnabled(enabled: Boolean) {
        audioVisualizer.setBeatDetectionEnabled(enabled)
    }
    
    /**
     * Updates the beat detection configuration.
     * This allows fine-tuning of sensitivity and other parameters.
     */
    fun setBeatDetectionConfig(config: BeatDetectionConfig) {
        audioVisualizer.setBeatDetectionConfig(config)
    }
    
    internal fun release() {
        audioVisualizer.release()
    }
}