package com.example.audiovisualizer.dream

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.room.Room
import com.example.audiovisualizer.data.database.AppDatabase
import com.example.audiovisualizer.data.repository.SettingsRepository
import com.tayadehritik.audiovisualizer.AudioVisualizerState
import com.tayadehritik.audiovisualizer.BeatDetectionConfig
import com.tayadehritik.audiovisualizer.FrequencyBand
import com.tayadehritik.audiovisualizer.createAudioVisualizer
import com.tayadehritik.audiovisualizer.releaseAudioVisualizer
import com.tayadehritik.audiovisualizer.visualizers.AudioBarsVisualizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AudioVisualizerDreamService : DreamServiceCompat() {
    
    companion object {
        private const val TAG = "AudioVisualizerDream"
    }
    
    private var visualizerState: AudioVisualizerState? = null
    private var database: AppDatabase? = null
    private var settingsRepository: SettingsRepository? = null
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow")
        
        // Set up the dream
        isInteractive = false  // No touch interaction needed for visualizer
        isFullscreen = true
        
        // Initialize database and repository
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
        
        settingsRepository = SettingsRepository(database!!.visualizerSettingsDao())
        
        // Initialize audio visualizer
        try {
            visualizerState = createAudioVisualizer(audioSessionId = 0)
            
            if (visualizerState == null) {
                Log.e(TAG, "Failed to create audio visualizer")
                return
            }
            
            // Set the Compose content using the DreamServiceCompat method
            setComposeContent {
                val settings by settingsRepository!!.settings.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                
                // Apply beat detection settings when they're loaded
                LaunchedEffect(settings) {
                    settings?.let { currentSettings ->
                        visualizerState?.let { state ->
                            state.setBeatDetectionEnabled(currentSettings.beatDetectionEnabled)
                            if (currentSettings.beatDetectionEnabled) {
                                val frequencyBand = FrequencyBand.values()
                                    .find { it.name == currentSettings.beatFrequencyBand } 
                                    ?: FrequencyBand.ALL_FREQUENCIES
                                    
                                state.setBeatDetectionConfig(
                                    BeatDetectionConfig(
                                        sensitivity = currentSettings.beatSensitivity,
                                        smoothingFactor = currentSettings.beatSmoothingFactor,
                                        frequencyBand = frequencyBand
                                    )
                                )
                            }
                        }
                    }
                }
                
                MaterialTheme {
                    Surface(modifier = Modifier.background(Color.Black)) {
                        // Use the bar visualizer with user settings
                        AudioBarsVisualizer(
                            state = visualizerState!!,
                            modifier = Modifier.fillMaxSize(),
                            barCount = settings?.barCount ?: 32,
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
        
        // Close database
        database?.close()
        database = null
        settingsRepository = null
    }
}