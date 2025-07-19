package com.tayadehritik.audiovisualizer

import android.media.audiofx.Visualizer
import android.util.Log

@Suppress("ktlint:standard:no-trailing-spaces")
class AudioVisualizer(private val audioSessionId: Int) {
    companion object {
        private const val TAG = "AudioVisualizer"
    }

    private var visualizer: Visualizer? = null
    private var latestFftData: ByteArray? = null
    
    fun initialize() {
        try {
            Log.d(TAG, "Initializing AudioVisualizer with session ID: $audioSessionId")
            
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1] // Max size
                
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) {
                            // Ignore waveform for now
                        }
                    
                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int,
                        ) {
                            fft?.let {
                                latestFftData = it
                                Log.d(TAG, "FFT data captured: ${it.size} bytes")
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true,
                )
                
                Log.d(TAG, "Visualizer created successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize visualizer", e)
            throw e
        }
    }
    
    fun start() {
        try {
            visualizer?.enabled = true
            Log.d(TAG, "Visualizer started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start visualizer", e)
            throw e
        }
    }
    
    fun stop() {
        visualizer?.enabled = false
        Log.d(TAG, "Visualizer stopped")
    }
    
    fun release() {
        stop()
        visualizer?.release()
        visualizer = null
        Log.d(TAG, "Visualizer released")
    }
    
    fun getLatestFftData(): ByteArray? = latestFftData
}
