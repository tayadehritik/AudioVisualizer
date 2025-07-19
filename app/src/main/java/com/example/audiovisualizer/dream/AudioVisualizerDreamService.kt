package com.example.audiovisualizer.dream

import android.service.dreams.DreamService
import android.util.Log
import android.widget.FrameLayout

class AudioVisualizerDreamService : DreamService() {
    
    companion object {
        private const val TAG = "AudioVisualizerDream"
    }
    
    private var audioVisualizer: AudioVisualizerManager? = null
    private var audioBarsView: AudioBarsView? = null
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow")
        
        // Set up the dream
        isInteractive = false  // No touch interaction needed for visualizer
        isFullscreen = true
        
        // Create the layout
        val frameLayout = FrameLayout(this)
        setContentView(frameLayout)
        
        // Create and add the visualizer view
        audioBarsView = AudioBarsView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        frameLayout.addView(audioBarsView)
        
        // Initialize audio visualizer
        try {
            audioVisualizer = AudioVisualizerManager(0).apply {
                initialize()
            }
            
            // Connect visualizer data to view
            audioVisualizer?.fftDataFlow?.let { flow ->
                audioBarsView?.setFftDataFlow(flow)
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
            audioVisualizer?.start()
            audioBarsView?.startAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start visualizer", e)
        }
    }
    
    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Log.d(TAG, "onDreamingStopped")
        
        // Stop the visualizer
        audioBarsView?.stopAnimation()
        audioVisualizer?.stop()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow")
        
        // Clean up resources
        audioBarsView?.cleanup()
        audioBarsView = null
        
        audioVisualizer?.release()
        audioVisualizer = null
    }
}