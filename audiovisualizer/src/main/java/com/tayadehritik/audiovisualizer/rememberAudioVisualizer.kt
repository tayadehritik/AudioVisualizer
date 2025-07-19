package com.tayadehritik.audiovisualizer

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Creates and remembers an [AudioVisualizerState] that automatically manages the lifecycle
 * of an Android Visualizer.
 *
 * @param audioSessionId The audio session ID to attach to. Use 0 for the system mix.
 * @param enabled Whether the visualizer should be actively capturing data.
 * @return An [AudioVisualizerState] that provides access to FFT data.
 *
 * Example usage:
 * ```
 * val visualizerState = rememberAudioVisualizer(
 *     audioSessionId = mediaPlayer.audioSessionId,
 *     enabled = isPlaying
 * )
 *
 * // Access FFT data
 * val fftData by visualizerState.fftData.collectAsState()
 * ```
 */
@Composable
fun rememberAudioVisualizer(
    audioSessionId: Int,
    enabled: Boolean = true
): AudioVisualizerState? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var error by remember { mutableStateOf<String?>(null) }
    
    val state = remember(audioSessionId) {
        try {
            val visualizer = AudioVisualizer(audioSessionId)
            visualizer.initialize()
            AudioVisualizerState(visualizer).also {
                Log.d("rememberAudioVisualizer", "AudioVisualizer created for session $audioSessionId")
            }
        } catch (e: Exception) {
            Log.e("rememberAudioVisualizer", "Failed to create AudioVisualizer", e)
            error = e.message
            null
        }
    }
    
    // Manage lifecycle
    DisposableEffect(state, lifecycleOwner, enabled) {
        if (state == null) {
            return@DisposableEffect onDispose { }
        }
        
        // Start if enabled and lifecycle is resumed
        if (enabled && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            state.resume()
        }
        
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (enabled) {
                        state.resume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    state.pause()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.release()
            Log.d("rememberAudioVisualizer", "AudioVisualizer released")
        }
    }
    
    // Handle enabled state changes
    DisposableEffect(enabled, state) {
        if (state != null && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (enabled) {
                state.resume()
            } else {
                state.pause()
            }
        }
        onDispose { }
    }
    
    return state
}