package com.tayadehritik.audiovisualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("ktlint:standard:no-trailing-spaces")
class AudioVisualizer(private val audioSessionId: Int) {
    companion object {
        private const val TAG = "AudioVisualizer"
    }

    private var visualizer: Visualizer? = null
    private val _fftDataFlow = MutableStateFlow<ByteArray?>(null)
    val fftDataFlow: StateFlow<ByteArray?> = _fftDataFlow.asStateFlow()
    
    init {
        initialize()
        start()
    }
    
    private fun initialize() {
        try {
            Log.d(TAG, "Initializing AudioVisualizer with session ID: $audioSessionId")
            
            visualizer = Visualizer(audioSessionId).apply {
                // Ensure visualizer is disabled before configuration
                setEnabled(false)
                
                // Set capture size to maximum available
                val captureSizeRange = Visualizer.getCaptureSizeRange()
                val captureSize = captureSizeRange[1] // Max size
                val result = setCaptureSize(captureSize)
                if (result != Visualizer.SUCCESS) {
                    Log.e(TAG, "Failed to set capture size. Error code: $result")
                } else {
                    Log.d(TAG, "Capture size set to: $captureSize")
                }
                
                // Set data capture listener
                val listenerResult = setDataCaptureListener(
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
                                // Create a copy of the array to ensure StateFlow detects the change
                                _fftDataFlow.value = it.copyOf()
                                Log.d(TAG, "FFT data captured: ${it.size} bytes, samplingRate: $samplingRate")
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false, // waveform
                    true,  // fft
                )
                
                if (listenerResult != Visualizer.SUCCESS) {
                    Log.e(TAG, "Failed to set data capture listener. Error code: $listenerResult")
                }
                
                Log.d(TAG, "Visualizer created successfully. Capture rate: ${Visualizer.getMaxCaptureRate() / 2}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize visualizer", e)
            throw e
        }
    }
    
    private fun start() {
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
    
}
