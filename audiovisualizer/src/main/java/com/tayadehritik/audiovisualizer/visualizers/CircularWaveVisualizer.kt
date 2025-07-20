package com.tayadehritik.audiovisualizer.visualizers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.tayadehritik.audiovisualizer.AudioVisualizerState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A composable that displays audio frequency data as a smooth circular wave visualization.
 * Creates a continuous wave pattern around a circle, similar to many music player visualizers.
 *
 * @param state The [AudioVisualizerState] providing FFT data
 * @param modifier Modifier for this composable
 * @param points Number of points to use for the wave (higher = smoother)
 * @param innerRadiusFraction Base inner radius as a fraction of canvas size
 * @param amplitudeFraction Maximum amplitude as a fraction of the radius
 * @param strokeWidth Width of the wave line
 * @param waveColor Color of the wave
 * @param fillWave Whether to fill the wave area
 * @param backgroundColor Background color
 * @param rotationSpeed Rotation speed in degrees per second
 */
@Composable
fun CircularWaveVisualizer(
    state: AudioVisualizerState,
    modifier: Modifier = Modifier,
    points: Int = 128,
    innerRadiusFraction: Float = 0.3f,
    amplitudeFraction: Float = 0.3f,
    strokeWidth: Float = 3f,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    fillWave: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    rotationSpeed: Float = 10f
) {
    val fftData by state.fftData.collectAsState()
    
    // Process FFT data into wave amplitudes
    val targetAmplitudes = remember(fftData) {
        processWaveAmplitudes(fftData, points)
    }
    
    // Animate each amplitude
    val animatedAmplitudes = targetAmplitudes.map { amplitude ->
        animateFloatAsState(
            targetValue = amplitude,
            animationSpec = tween(durationMillis = 150),
            label = "wave_animation"
        ).value
    }
    
    // Calculate rotation
    val currentTime = System.currentTimeMillis()
    val rotation = if (rotationSpeed != 0f) {
        ((currentTime * rotationSpeed / 1000f) % 360f)
    } else {
        0f
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(backgroundColor, size = size)
        
        if (animatedAmplitudes.isNotEmpty()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = minOf(size.width, size.height) / 2f
            val baseRadius = maxRadius * innerRadiusFraction
            val maxAmplitude = maxRadius * amplitudeFraction
            
            rotate(rotation, pivot = Offset(centerX, centerY)) {
                drawCircularWave(
                    centerX = centerX,
                    centerY = centerY,
                    baseRadius = baseRadius,
                    amplitudes = animatedAmplitudes,
                    maxAmplitude = maxAmplitude,
                    color = waveColor,
                    strokeWidth = strokeWidth,
                    fill = fillWave
                )
            }
        }
    }
}

/**
 * Draw the circular wave using a Path
 */
private fun DrawScope.drawCircularWave(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    amplitudes: List<Float>,
    maxAmplitude: Float,
    color: Color,
    strokeWidth: Float,
    fill: Boolean
) {
    val path = Path()
    val angleStep = (2 * PI / amplitudes.size).toFloat()
    
    // Create the wave path
    amplitudes.forEachIndexed { index, amplitude ->
        val angle = index * angleStep
        val radius = baseRadius + (amplitude * maxAmplitude)
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    // Close the path
    path.close()
    
    // Draw the wave
    if (fill) {
        drawPath(
            path = path,
            color = color.copy(alpha = 0.5f),
            style = Fill
        )
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Process FFT data into smooth wave amplitudes
 */
private fun processWaveAmplitudes(fftData: ByteArray?, points: Int): List<Float> {
    if (fftData == null || fftData.isEmpty()) {
        return List(points) { 0f }
    }
    
    // Extract magnitudes from FFT data
    val magnitudes = mutableListOf<Float>()
    val halfSize = fftData.size / 2
    
    for (i in 2 until halfSize step 2) {
        val real = fftData[i].toFloat()
        val imaginary = fftData[i + 1].toFloat()
        val magnitude = sqrt(real * real + imaginary * imaginary)
        magnitudes.add(magnitude)
    }
    
    // Resample to match point count
    val amplitudes = mutableListOf<Float>()
    val step = magnitudes.size.toFloat() / points
    
    for (i in 0 until points) {
        val index = (i * step).toInt().coerceIn(0, magnitudes.size - 1)
        val magnitude = magnitudes[index]
        
        // Convert to dB and normalize
        val db = 20 * log10(magnitude + 1f)
        val normalized = (db / 50f).coerceIn(0f, 1f)
        amplitudes.add(normalized)
    }
    
    // Apply heavy smoothing for wave effect
    return heavySmoothAmplitudes(amplitudes, 3)
}

/**
 * Apply multiple passes of smoothing for a flowing wave effect
 */
private fun heavySmoothAmplitudes(amplitudes: List<Float>, passes: Int): List<Float> {
    var result = amplitudes
    
    repeat(passes) {
        val smoothed = mutableListOf<Float>()
        
        for (i in result.indices) {
            val prev2 = result[(i - 2 + result.size) % result.size]
            val prev1 = result[(i - 1 + result.size) % result.size]
            val current = result[i]
            val next1 = result[(i + 1) % result.size]
            val next2 = result[(i + 2) % result.size]
            
            // Weighted average for smoother transitions
            val smoothedValue = (
                prev2 * 0.1f + 
                prev1 * 0.2f + 
                current * 0.4f + 
                next1 * 0.2f + 
                next2 * 0.1f
            )
            smoothed.add(smoothedValue)
        }
        
        result = smoothed
    }
    
    return result
}