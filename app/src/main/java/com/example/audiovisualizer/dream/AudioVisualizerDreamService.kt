package com.example.audiovisualizer.dream

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tayadehritik.audiovisualizer.AudioVisualizerState
import com.tayadehritik.audiovisualizer.createAudioVisualizer
import com.tayadehritik.audiovisualizer.releaseAudioVisualizer
import com.tayadehritik.audiovisualizer.visualizers.AudioBarsVisualizer
import com.tayadehritik.audiovisualizer.visualizers.CircularBarsVisualizer

class AudioVisualizerDreamService : DreamServiceCompat() {
    
    companion object {
        private const val TAG = "AudioVisualizerDream"
    }
    
    private var visualizerState: AudioVisualizerState? = null
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow")
        
        // Set up the dream
        isInteractive = false  // No touch interaction needed for visualizer
        isFullscreen = true
        
        // Initialize audio visualizer
        try {
            visualizerState = createAudioVisualizer(audioSessionId = 0)
            
            if (visualizerState == null) {
                Log.e(TAG, "Failed to create audio visualizer")
                return
            }
            
            // Set the Compose content using the DreamServiceCompat method
            setComposeContent {
                MaterialTheme {
                    Surface(modifier = Modifier.background(Color.Black)) {
                        // Use the circular visualizer with mirrored effect
                        AudioBarsVisualizer(
                            state = visualizerState!!,
                            modifier = Modifier.fillMaxSize(),
                            barCount = 32,
                            barColor = MaterialTheme.colorScheme.primary,
                            backgroundColor = Color.Black,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio visualizer", e)
        }
    }
    
    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Log.d(TAG, "onDreamingStarted")
        
        // Start the visualizer
        try {
            visualizerState?.resume()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start visualizer", e)
        }
    }
    
    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Log.d(TAG, "onDreamingStopped")
        
        // Pause the visualizer
        visualizerState?.pause()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow")
        
        // Clean up resources
        visualizerState?.let {
            releaseAudioVisualizer(it)
        }
        visualizerState = null
    }
}