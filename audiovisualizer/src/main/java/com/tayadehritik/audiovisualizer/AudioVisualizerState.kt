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
    
    internal fun release() {
        audioVisualizer.release()
    }
}