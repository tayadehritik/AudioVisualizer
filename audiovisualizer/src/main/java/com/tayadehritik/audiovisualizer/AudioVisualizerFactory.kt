package com.tayadehritik.audiovisualizer

import android.util.Log

/**
 * Factory function to create an [AudioVisualizerState] instance for use outside of Compose.
 * This is useful for traditional Android Views, Services, or any non-Composable context.
 *
 * @param audioSessionId The audio session ID to attach to. Use 0 for the system mix.
 * @return An [AudioVisualizerState] instance, or null if initialization fails.
 *
 * Important: The caller is responsible for managing the lifecycle:
 * - Call [AudioVisualizerState.resume] to start visualization
 * - Call [AudioVisualizerState.pause] to stop visualization
 * - Call [releaseAudioVisualizer] when done to free resources
 *
 * Example usage:
 * ```
 * class MyService : Service() {
 *     private var visualizerState: AudioVisualizerState? = null
 *
 *     override fun onCreate() {
 *         visualizerState = createAudioVisualizer(0)
 *         visualizerState?.resume()
 *     }
 *
 *     override fun onDestroy() {
 *         visualizerState?.let { releaseAudioVisualizer(it) }
 *     }
 * }
 * ```
 */
fun createAudioVisualizer(audioSessionId: Int): AudioVisualizerState? {
    return try {
        val visualizer = AudioVisualizer(audioSessionId)
        visualizer.initialize()
        AudioVisualizerState(visualizer).also {
            Log.d("AudioVisualizerFactory", "AudioVisualizer created for session $audioSessionId")
        }
    } catch (e: Exception) {
        Log.e("AudioVisualizerFactory", "Failed to create AudioVisualizer", e)
        null
    }
}

/**
 * Releases the resources associated with an [AudioVisualizerState] created by [createAudioVisualizer].
 * This should be called when the visualizer is no longer needed.
 *
 * @param state The [AudioVisualizerState] to release.
 */
fun releaseAudioVisualizer(state: AudioVisualizerState) {
    try {
        state.release()
        Log.d("AudioVisualizerFactory", "AudioVisualizer released")
    } catch (e: Exception) {
        Log.e("AudioVisualizerFactory", "Error releasing AudioVisualizer", e)
    }
}