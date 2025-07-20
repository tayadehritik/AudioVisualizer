package com.tayadehritik.audiovisualizer

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("ktlint:standard:no-trailing-spaces")
internal class AudioVisualizer(private val audioSessionId: Int) {
    companion object {
        private const val TAG = "AudioVisualizer"
    }

    private var visualizer: Visualizer? = null
    private val _fftDataFlow = MutableStateFlow<ByteArray?>(null)
    val fftDataFlow: StateFlow<ByteArray?> = _fftDataFlow.asStateFlow()
    
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private val _beatFlow = MutableStateFlow<BeatEvent?>(null)
    val beatFlow: StateFlow<BeatEvent?> = _beatFlow.asStateFlow()
    
    private val _beatDetectionState = MutableStateFlow(BeatDetectionState())
    val beatDetectionState: StateFlow<BeatDetectionState> = _beatDetectionState.asStateFlow()
    
    private var fftCallbackCount = 0
    private var beatDetectionConfig = BeatDetectionConfig()
    private val energyHistory = EnergyHistory(beatDetectionConfig.historyWindowMs)
    private var lastBeatTime = 0L
    private var smoothedIntensity = 0f
    
    fun initialize() {
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
                            fftCallbackCount++
                            Log.v(TAG, "onFftDataCapture called (#$fftCallbackCount) - fft is ${if (fft == null) "NULL" else "NOT NULL"}")
                            
                            if (fft == null) {
                                Log.w(TAG, "FFT data is null!")
                                return
                            }
                            
                            // Create a copy of the array to ensure StateFlow detects the change
                            _fftDataFlow.value = fft.copyOf()
                            
                            // Process beat detection if enabled
                            if (_beatDetectionState.value.isEnabled) {
                                processBeatDetection(fft)
                            }
                            
                            // Log detailed FFT information
                            Log.d(TAG, "FFT data captured: ${fft.size} bytes, samplingRate: $samplingRate")
                            
                            // Log first few FFT values for debugging
                            if (fft.isNotEmpty()) {
                                val firstValues = fft.take(10).joinToString { 
                                    String.format("%02X", it.toInt() and 0xFF) 
                                }
                                Log.v(TAG, "First 10 FFT values (hex): $firstValues")
                                
                                // Check if all values are zero
                                val allZero = fft.all { it.toInt() == 0 }
                                if (allZero) {
                                    Log.w(TAG, "All FFT values are zero!")
                                }
                            }
                            
                            // Log periodically to avoid log spam
                            if (fftCallbackCount % 10 == 0) {
                                Log.i(TAG, "FFT callback count: $fftCallbackCount")
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
    
    fun start() {
        try {
            if (visualizer == null) {
                Log.e(TAG, "Cannot start - visualizer is null!")
                throw IllegalStateException("Visualizer not initialized")
            }
            
            visualizer?.enabled = true
            _isActive.value = true
            
            // Log visualizer state
            val enabled = visualizer?.enabled ?: false
            val captureSize = visualizer?.captureSize ?: -1
            val samplingRate = visualizer?.samplingRate ?: -1
            
            Log.d(TAG, "Visualizer started - enabled: $enabled, captureSize: $captureSize, samplingRate: $samplingRate")
            Log.d(TAG, "Audio session ID: $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start visualizer", e)
            throw e
        }
    }
    
    fun stop() {
        visualizer?.enabled = false
        _isActive.value = false
        Log.d(TAG, "Visualizer stopped")
    }
    
    fun release() {
        stop()
        visualizer?.release()
        visualizer = null
        Log.d(TAG, "Visualizer released")
    }
    
    fun setBeatDetectionEnabled(enabled: Boolean) {
        _beatDetectionState.value = _beatDetectionState.value.copy(isEnabled = enabled)
        if (!enabled) {
            energyHistory.clear()
            smoothedIntensity = 0f
        }
    }
    
    fun setBeatDetectionConfig(config: BeatDetectionConfig) {
        beatDetectionConfig = config
    }
    
    private fun processBeatDetection(fft: ByteArray) {
        // Calculate energy from FFT magnitude data
        val energy = calculateEnergy(fft)
        
        // Add to history
        energyHistory.addSample(energy)
        
        // Get statistics
        val avgEnergy = energyHistory.getAverage()
        val variance = energyHistory.getVariance()
        val stdDev = sqrt(variance)
        
        // Update state
        _beatDetectionState.value = _beatDetectionState.value.copy(
            currentEnergy = energy,
            averageEnergy = avgEnergy,
            energyVariance = variance
        )
        
        // Beat detection using dynamic threshold
        val threshold = avgEnergy + (beatDetectionConfig.sensitivity * stdDev)
        val currentTime = System.currentTimeMillis()
        
        if (energy > threshold && 
            currentTime - lastBeatTime > beatDetectionConfig.ignoreTimeMs) {
            
            // Calculate beat intensity (0-1 range)
            val rawIntensity = if (stdDev > 0) {
                ((energy - avgEnergy) / stdDev).coerceIn(0f, 3f) / 3f
            } else {
                0f
            }
            
            // Apply smoothing
            smoothedIntensity = smoothedIntensity * beatDetectionConfig.smoothingFactor + 
                               rawIntensity * (1f - beatDetectionConfig.smoothingFactor)
            
            // Emit beat event
            _beatFlow.value = BeatEvent(
                timestamp = currentTime,
                intensity = smoothedIntensity,
                frequency = 0f // Could calculate dominant frequency if needed
            )
            
            lastBeatTime = currentTime
            
            _beatDetectionState.value = _beatDetectionState.value.copy(
                lastBeatTimestamp = currentTime,
                beatIntensity = smoothedIntensity
            )
            
            Log.v(TAG, "Beat detected! Energy: $energy, Avg: $avgEnergy, Intensity: $smoothedIntensity")
        } else {
            // Decay intensity when no beat
            smoothedIntensity *= 0.95f
            _beatDetectionState.value = _beatDetectionState.value.copy(
                beatIntensity = smoothedIntensity
            )
        }
    }
    
    private fun calculateEnergy(fft: ByteArray): Float {
        // FFT data is in format: [real0, imag0, real1, imag1, ...]
        // We only need magnitude from first half (second half is mirror)
        var totalEnergy = 0.0
        val halfSize = fft.size / 2
        
        for (i in 0 until halfSize step 2) {
            if (i + 1 < fft.size) {
                val real = fft[i].toFloat()
                val imag = fft[i + 1].toFloat()
                val magnitude = sqrt(real.pow(2) + imag.pow(2))
                totalEnergy += magnitude.toDouble()
            }
        }
        
        return (totalEnergy / (halfSize / 2)).toFloat()
    }
    
}
