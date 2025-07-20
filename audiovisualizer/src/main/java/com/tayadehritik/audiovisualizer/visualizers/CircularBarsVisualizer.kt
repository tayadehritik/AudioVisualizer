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
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.tayadehritik.audiovisualizer.AudioVisualizerState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A composable that displays audio frequency data as circular bars radiating from the center.
 * Inspired by osu!'s logo visualizer.
 *
 * @param state The [AudioVisualizerState] providing FFT data
 * @param modifier Modifier for this composable
 * @param barCount Number of bars in the circle (more bars = smoother circle)
 * @param innerRadiusFraction Inner radius as fraction of available space (0.0 to 1.0)
 * @param maxBarHeightFraction Maximum bar height as fraction of available radius
 * @param barColor Color of the bars
 * @param backgroundColor Background color
 * @param rotationSpeed Speed of rotation in degrees per second (0 for no rotation)
 * @param mirrored Whether to mirror the visualization (draw bars both inward and outward)
 */
@Composable
fun CircularBarsVisualizer(
    state: AudioVisualizerState,
    modifier: Modifier = Modifier,
    barCount: Int = 128,
    innerRadiusFraction: Float = 0.3f,
    maxBarHeightFraction: Float = 0.4f,
    barColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    rotationSpeed: Float = 0f,
    mirrored: Boolean = false
) {
    val fftData by state.fftData.collectAsState()
    
    // Process FFT data into normalized heights
    val targetHeights = remember(fftData) {
        processFftDataForCircular(fftData, barCount)
    }
    
    // Animate each bar height
    val animatedHeights = targetHeights.map { targetHeight ->
        animateFloatAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 50),
            label = "circular_bar_animation"
        ).value
    }
    
    // Rotation animation
    val rotation by animateFloatAsState(
        targetValue = if (rotationSpeed != 0f) 360f else 0f,
        animationSpec = tween(
            durationMillis = if (rotationSpeed != 0f) (360000 / rotationSpeed).toInt() else 1000,
            delayMillis = 0
        ),
        label = "rotation_animation"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(backgroundColor, size = size)
        
        val centerX = size.center.x
        val centerY = size.center.y
        val maxRadius = min(centerX, centerY)
        val innerRadius = maxRadius * innerRadiusFraction
        val maxBarHeight = maxRadius * maxBarHeightFraction
        
        // Calculate bar properties
        val angleStep = 360f / barCount
        val barWidth = (2 * PI * innerRadius / barCount).toFloat() * 0.8f // 80% width for spacing
        
        // Apply rotation to the entire visualization
        rotate(rotation, pivot = size.center) {
            animatedHeights.forEachIndexed { index, normalizedHeight ->
                val angle = index * angleStep
                val angleRad = Math.toRadians(angle.toDouble())
                
                // Calculate bar position
                val barHeight = normalizedHeight * maxBarHeight
                val startRadius = innerRadius
                val endRadius = innerRadius + barHeight
                
                // Draw outward bar
                drawBar(
                    centerX = centerX,
                    centerY = centerY,
                    angle = angleRad,
                    startRadius = startRadius,
                    endRadius = endRadius,
                    barWidth = barWidth,
                    color = barColor
                )
                
                // Draw mirrored inward bar if enabled
                if (mirrored && barHeight > 0) {
                    drawBar(
                        centerX = centerX,
                        centerY = centerY,
                        angle = angleRad,
                        startRadius = innerRadius,
                        endRadius = innerRadius - barHeight * 0.5f, // Inward bars are shorter
                        barWidth = barWidth,
                        color = barColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Optional: Draw center circle
        drawCircle(
            color = barColor.copy(alpha = 0.1f),
            radius = innerRadius * 0.9f,
            center = size.center
        )
    }
}

/**
 * Extension function to draw a bar at a specific angle
 */
private fun DrawScope.drawBar(
    centerX: Float,
    centerY: Float,
    angle: Double,
    startRadius: Float,
    endRadius: Float,
    barWidth: Float,
    color: Color
) {
    val cosAngle = cos(angle).toFloat()
    val sinAngle = sin(angle).toFloat()
    
    // Calculate perpendicular offset for bar width
    val perpCos = cos(angle + PI / 2).toFloat()
    val perpSin = sin(angle + PI / 2).toFloat()
    val halfWidth = barWidth / 2
    
    // Calculate four corners of the bar
    val x1 = centerX + startRadius * cosAngle - halfWidth * perpCos
    val y1 = centerY + startRadius * sinAngle - halfWidth * perpSin
    
    val x2 = centerX + startRadius * cosAngle + halfWidth * perpCos
    val y2 = centerY + startRadius * sinAngle + halfWidth * perpSin
    
    val x3 = centerX + endRadius * cosAngle + halfWidth * perpCos
    val y3 = centerY + endRadius * sinAngle + halfWidth * perpSin
    
    val x4 = centerX + endRadius * cosAngle - halfWidth * perpCos
    val y4 = centerY + endRadius * sinAngle - halfWidth * perpSin
    
    // Draw as a path (quad)
    drawLine(
        color = color,
        start = Offset(x1, y1),
        end = Offset(x4, y4),
        strokeWidth = barWidth
    )
}

/**
 * Process FFT data for circular visualization
 * Uses logarithmic scaling for better visual representation
 */
private fun processFftDataForCircular(fftData: ByteArray?, barCount: Int): List<Float> {
    if (fftData == null || fftData.isEmpty()) {
        return List(barCount) { 0f }
    }
    
    // FFT data is complex numbers (real, imaginary pairs)
    val magnitudes = mutableListOf<Float>()
    val halfSize = fftData.size / 2
    
    // Skip DC component and process only useful frequencies
    for (i in 2 until halfSize step 2) {
        val real = fftData[i].toFloat()
        val imaginary = fftData[i + 1].toFloat()
        val magnitude = sqrt(real * real + imaginary * imaginary)
        magnitudes.add(magnitude)
    }
    
    // Group frequencies into bars with logarithmic scaling
    val heights = mutableListOf<Float>()
    val samplesPerBar = magnitudes.size / barCount
    
    for (i in 0 until barCount) {
        val startIdx = i * samplesPerBar
        val endIdx = minOf(startIdx + samplesPerBar, magnitudes.size)
        
        if (startIdx < magnitudes.size) {
            // Average the magnitudes in this frequency range
            val avg = magnitudes.subList(startIdx, endIdx).average().toFloat()
            
            // Apply logarithmic scaling for better visual effect
            val db = 20 * log10(avg + 1f)
            val normalized = (db / 60f).coerceIn(0f, 1f)
            
            // Apply smoothing based on frequency (lower frequencies are usually stronger)
            val frequencyFactor = 1f - (i.toFloat() / barCount) * 0.3f
            heights.add(normalized * frequencyFactor)
        } else {
            heights.add(0f)
        }
    }
    
    return heights
}