package com.example.audiovisualizer.dream

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AudioBarsView(context: Context) : View(context) {
    
    companion object {
        private const val TAG = "AudioBarsView"
        private const val BAR_COUNT = 32
        private const val ANIMATION_DURATION = 100L
    }
    
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val barHeights = FloatArray(BAR_COUNT) { 0f }
    private val targetHeights = FloatArray(BAR_COUNT) { 0f }
    private val animators = mutableListOf<ValueAnimator>()
    
    private var coroutineScope: CoroutineScope? = null
    private var fftCollectionJob: Job? = null
    
    // Colors - using a gradient effect
    private val barColor = Color.parseColor("#2196F3") // Material Blue
    private val backgroundColor = Color.parseColor("#121212") // Dark background
    
    init {
        setBackgroundColor(backgroundColor)
        coroutineScope = CoroutineScope(Dispatchers.Main)
    }
    
    fun setFftDataFlow(fftDataFlow: StateFlow<ByteArray?>) {
        Log.d(TAG, "Setting FFT data flow")
        
        fftCollectionJob?.cancel()
        fftCollectionJob = coroutineScope?.launch {
            fftDataFlow.collectLatest { fftData ->
                if (fftData != null) {
                    Log.v(TAG, "Received FFT data: ${fftData.size} bytes")
                    processFftData(fftData)
                }
            }
        }
    }
    
    private fun processFftData(fftData: ByteArray) {
        if (fftData.isEmpty()) return
        
        // FFT data is complex numbers (real, imaginary pairs)
        val magnitudes = mutableListOf<Float>()
        val halfSize = fftData.size / 2
        
        // Skip first pair (DC component) and process only first half
        for (i in 2 until halfSize step 2) {
            val real = fftData[i].toFloat()
            val imaginary = fftData[i + 1].toFloat()
            val magnitude = sqrt(real * real + imaginary * imaginary)
            magnitudes.add(magnitude)
        }
        
        // Group frequencies into bars
        val samplesPerBar = magnitudes.size / BAR_COUNT
        
        for (i in 0 until BAR_COUNT) {
            val startIdx = i * samplesPerBar
            val endIdx = minOf(startIdx + samplesPerBar, magnitudes.size)
            
            if (startIdx < magnitudes.size) {
                // Average the magnitudes in this frequency range
                val avg = if (endIdx > startIdx) {
                    magnitudes.subList(startIdx, endIdx).average().toFloat()
                } else {
                    0f
                }
                
                // Convert to dB and normalize
                val db = 20 * log10(avg + 1f)
                val normalized = (db / 80f).coerceIn(0f, 1f)
                targetHeights[i] = normalized
            } else {
                targetHeights[i] = 0f
            }
        }
        
        // Update the view
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (width == 0 || height == 0) return
        
        val barWidth = width.toFloat() / BAR_COUNT
        val barSpacing = barWidth * 0.1f
        val actualBarWidth = barWidth - barSpacing
        
        // Animate heights
        for (i in 0 until BAR_COUNT) {
            // Simple lerp animation
            barHeights[i] = barHeights[i] + (targetHeights[i] - barHeights[i]) * 0.2f
            
            val barHeight = barHeights[i] * height * 0.8f
            val left = i * barWidth + barSpacing / 2
            val top = height - barHeight
            
            // Draw bar with gradient effect (darker at bottom)
            val colorIntensity = 0.5f + (barHeights[i] * 0.5f)
            paint.color = adjustColorBrightness(barColor, colorIntensity)
            
            canvas.drawRect(
                left,
                top,
                left + actualBarWidth,
                height.toFloat(),
                paint
            )
        }
        
        // Continue animation if any bar is moving
        if (barHeights.indices.any { 
            kotlin.math.abs(barHeights[it] - targetHeights[it]) > 0.01f 
        }) {
            postInvalidateOnAnimation()
        }
    }
    
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = ((Color.red(color) * factor).toInt()).coerceIn(0, 255)
        val g = ((Color.green(color) * factor).toInt()).coerceIn(0, 255)
        val b = ((Color.blue(color) * factor).toInt()).coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
    
    fun startAnimation() {
        Log.d(TAG, "Starting animation")
        // Animation is handled in onDraw
    }
    
    fun stopAnimation() {
        Log.d(TAG, "Stopping animation")
        animators.forEach { it.cancel() }
        animators.clear()
    }
    
    fun cleanup() {
        Log.d(TAG, "Cleaning up")
        stopAnimation()
        fftCollectionJob?.cancel()
        fftCollectionJob = null
        coroutineScope = null
    }
}