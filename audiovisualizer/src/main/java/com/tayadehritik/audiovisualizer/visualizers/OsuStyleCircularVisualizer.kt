package com.tayadehritik.audiovisualizer.visualizers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.tayadehritik.audiovisualizer.AudioVisualizerState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A composable that displays audio frequency data in the style of osu!'s logo visualizer.
 * Features individual bars arranged in a circular pattern with smooth animations and decay.
 *
 * @param state The [AudioVisualizerState] providing FFT data
 * @param modifier Modifier for this composable
 * @param barCount Number of bars in the visualizer (osu! uses 200)
 * @param visualizerRounds Number of times bars wrap around the circle (osu! uses 5)
 * @param innerRadiusFraction Inner radius as a fraction of canvas size
 * @param barLengthFraction Maximum bar length as a fraction of radius
 * @param barColor Base color for the bars
 * @param useGradient Whether to use gradient coloring
 * @param backgroundColor Background color
 * @param decayRate Rate at which bars decay when no audio (0.0 to 1.0)
 * @param sensitivity Audio sensitivity multiplier
 */
@Composable
fun OsuStyleCircularVisualizer(
    state: AudioVisualizerState,
    modifier: Modifier = Modifier,
    barCount: Int = 160,
    visualizerRounds: Int = 4,
    innerRadiusFraction: Float = 0.25f,
    barLengthFraction: Float = 0.4f,
    barColor: Color = MaterialTheme.colorScheme.primary,
    useGradient: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    decayRate: Float = 0.95f,
    sensitivity: Float = 1.2f
) {
    val fftData by state.fftData.collectAsState()
    
    // Store bar heights with decay
    var barHeights by remember { mutableStateOf(List(barCount) { 0f }) }
    
    // Process FFT data and apply decay
    LaunchedEffect(fftData) {
        val currentFftData = fftData
        if (currentFftData != null) {
            val newHeights = processOsuStyleFFT(currentFftData, barCount, sensitivity)
            // Blend new heights with decayed old heights
            barHeights = barHeights.mapIndexed { index, oldHeight ->
                maxOf(oldHeight * decayRate, newHeights[index])
            }
        } else {
            // Apply decay when no data
            barHeights = barHeights.map { it * decayRate }
        }
    }
    
    // Animate each bar height with spring animation for bouncy effect
    val animatedHeights = barHeights.map { targetHeight ->
        animateFloatAsState(
            targetValue = targetHeight,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 300f
            ),
            label = "osu_bar_animation"
        ).value
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(backgroundColor, size = size)
        
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = minOf(size.width, size.height) / 2f
        val innerRadius = maxRadius * innerRadiusFraction
        val maxBarLength = maxRadius * barLengthFraction
        
        // Draw the circular visualizer
        translate(centerX, centerY) {
            drawOsuBars(
                heights = animatedHeights,
                innerRadius = innerRadius,
                maxBarLength = maxBarLength,
                visualizerRounds = visualizerRounds,
                barColor = barColor,
                useGradient = useGradient
            )
        }
    }
}

/**
 * Draw bars in osu! style with multiple rounds
 */
private fun DrawScope.drawOsuBars(
    heights: List<Float>,
    innerRadius: Float,
    maxBarLength: Float,
    visualizerRounds: Int,
    barColor: Color,
    useGradient: Boolean
) {
    val totalBars = heights.size
    val degreesPerBar = (360f * visualizerRounds) / totalBars
    val barWidth = ((2 * PI * innerRadius * visualizerRounds) / totalBars * 0.7).toFloat() // 70% width for spacing
    
    heights.forEachIndexed { index, height ->
        val angle = index * degreesPerBar
        val angleRad = angle * PI / 180.0
        
        // Calculate bar dimensions
        val barLength = height * maxBarLength
        val barStart = innerRadius
        val barEnd = innerRadius + barLength
        
        // Create gradient or solid color
        val brush = if (useGradient) {
            Brush.radialGradient(
                colors = listOf(
                    barColor,
                    barColor.copy(alpha = 0.3f)
                ),
                center = Offset.Zero,
                radius = barEnd
            )
        } else {
            Brush.linearGradient(
                colors = listOf(barColor, barColor)
            )
        }
        
        // Draw the bar
        rotate(angle) {
            // Draw as a tapered rectangle for osu! style
            val path = Path().apply {
                moveTo(barStart, -barWidth / 2f)
                lineTo(barEnd, -barWidth / 3f)
                lineTo(barEnd, barWidth / 3f)
                lineTo(barStart, barWidth / 2f)
                close()
            }
            
            drawPath(
                path = path,
                brush = brush,
                style = Fill
            )
        }
    }
}

/**
 * Process FFT data in osu! style with frequency grouping and logarithmic scaling
 */
private fun processOsuStyleFFT(
    fftData: ByteArray,
    barCount: Int,
    sensitivity: Float
): List<Float> {
    // Extract magnitudes from FFT
    val magnitudes = mutableListOf<Float>()
    val halfSize = fftData.size / 2
    
    // Skip DC component
    for (i in 2 until halfSize step 2) {
        val real = fftData[i].toFloat()
        val imaginary = fftData[i + 1].toFloat()
        val magnitude = sqrt(real * real + imaginary * imaginary)
        magnitudes.add(magnitude)
    }
    
    // Group frequencies logarithmically for better visual distribution
    val heights = mutableListOf<Float>()
    val logBase = 1.05 // Logarithmic grouping base
    
    for (i in 0 until barCount) {
        // Calculate frequency range for this bar (logarithmic distribution)
        val freqStart = (Math.pow(logBase, i.toDouble()) - 1).toInt()
        val freqEnd = (Math.pow(logBase, (i + 1).toDouble()) - 1).toInt()
        
        val startIdx = (freqStart * magnitudes.size / 100).coerceIn(0, magnitudes.size - 1)
        val endIdx = (freqEnd * magnitudes.size / 100).coerceIn(startIdx, magnitudes.size - 1)
        
        if (startIdx < magnitudes.size && startIdx <= endIdx) {
            // Get the maximum magnitude in this frequency range
            val rangeMax = if (startIdx == endIdx) {
                magnitudes[startIdx]
            } else {
                magnitudes.subList(startIdx, endIdx + 1).maxOrNull() ?: 0f
            }
            
            // Convert to dB and normalize
            val db = 20 * log10(rangeMax + 1f)
            val normalized = (db / 45f * sensitivity).coerceIn(0f, 1f)
            heights.add(normalized)
        } else {
            heights.add(0f)
        }
    }
    
    // Apply smoothing for visual continuity
    return smoothOsuBars(heights)
}

/**
 * Smooth bar heights with wrap-around for circular continuity
 */
private fun smoothOsuBars(heights: List<Float>): List<Float> {
    val smoothed = mutableListOf<Float>()
    
    for (i in heights.indices) {
        val prev = heights[(i - 1 + heights.size) % heights.size]
        val current = heights[i]
        val next = heights[(i + 1) % heights.size]
        
        // Weighted smoothing with emphasis on current value
        val smoothedValue = prev * 0.15f + current * 0.7f + next * 0.15f
        smoothed.add(smoothedValue)
    }
    
    return smoothed
}