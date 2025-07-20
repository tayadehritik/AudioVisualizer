package com.tayadehritik.audiovisualizer.visualizers

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayadehritik.audiovisualizer.AudioVisualizerState
import java.nio.file.Files.size
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

private const val TAG = "AudioBarsVisualizer"

/**
 * A composable that displays audio frequency data as animated bars.
 *
 * @param state The [AudioVisualizerState] providing FFT data
 * @param modifier Modifier for this composable
 * @param barCount Number of frequency bars to display
 * @param barColor Color of the bars
 * @param backgroundColor Background color
 */
@Composable
fun AudioBarsVisualizer(
    state: AudioVisualizerState,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    barColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val fftData by state.fftData.collectAsState()
    val beatDetectionState by state.beatDetectionState.collectAsState()
    val density = LocalDensity.current
    
    // Process FFT data into bar heights
    val targetHeights = remember(fftData) {
        processFftData(fftData, barCount)
    }
    
    // Animate each bar height outside of Canvas
    val animatedHeights = targetHeights.map { targetHeight ->
        animateFloatAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 100),
            label = "bar_animation"
        ).value
    }
    
    // Animate beat intensity for pulse effect
    val animatedBeatIntensity by animateFloatAsState(
        targetValue = if (beatDetectionState.isEnabled) beatDetectionState.beatIntensity else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "beat_intensity"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(backgroundColor, size = size)
        
        if (animatedHeights.isNotEmpty()) {
            val barWidth = size.width / barCount
            val barSpacing = with(density) { 2.dp.toPx() }
            
            animatedHeights.forEachIndexed { index, normalizedHeight ->
                // Apply beat pulse effect
                val beatMultiplier = 1f + (animatedBeatIntensity * 0.3f)
                val animatedHeight = normalizedHeight * size.height * 0.8f * beatMultiplier
                
                val left = index * barWidth + barSpacing / 2
                val barActualWidth = barWidth - barSpacing
                val top = size.height - animatedHeight
                
                // Brighten color on beat
                val adjustedColor = if (beatDetectionState.isEnabled && animatedBeatIntensity > 0.1f) {
                    barColor.copy(
                        red = (barColor.red + animatedBeatIntensity * 0.3f).coerceIn(0f, 1f),
                        green = (barColor.green + animatedBeatIntensity * 0.3f).coerceIn(0f, 1f),
                        blue = (barColor.blue + animatedBeatIntensity * 0.3f).coerceIn(0f, 1f)
                    )
                } else {
                    barColor
                }
                
                drawRect(
                    color = adjustedColor,
                    topLeft = Offset(left, top),
                    size = Size(barActualWidth, animatedHeight)
                )
            }
        }
        
        // Draw beat indicator circle if beat detection is enabled
        if (beatDetectionState.isEnabled) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.3f // Position in upper portion
            val baseRadius = with(density) { 40.dp.toPx() }
            val maxRadius = with(density) { 80.dp.toPx() }
            
            // Calculate circle radius based on beat intensity
            val circleRadius = baseRadius + (animatedBeatIntensity * (maxRadius - baseRadius))
            
            // Draw outer glow when beat is detected
            if (animatedBeatIntensity > 0.1f) {
                val glowAlpha = (animatedBeatIntensity * 0.3f).coerceIn(0f, 0.3f)
                val glowRadius = circleRadius * 1.5f
                
                drawCircle(
                    color = barColor.copy(alpha = glowAlpha),
                    radius = glowRadius,
                    center = Offset(centerX, centerY)
                )
            }
            
            // Draw main circle
            val circleColor = if (animatedBeatIntensity > 0.1f) {
                barColor.copy(
                    red = (barColor.red + animatedBeatIntensity * 0.4f).coerceIn(0f, 1f),
                    green = (barColor.green + animatedBeatIntensity * 0.4f).coerceIn(0f, 1f),
                    blue = (barColor.blue + animatedBeatIntensity * 0.4f).coerceIn(0f, 1f)
                )
            } else {
                barColor.copy(alpha = 0.5f)
            }
            
            drawCircle(
                color = circleColor,
                radius = circleRadius,
                center = Offset(centerX, centerY)
            )
            
            // Draw inner circle for better visibility
            drawCircle(
                color = backgroundColor,
                radius = circleRadius * 0.8f,
                center = Offset(centerX, centerY)
            )
            
            // Draw center dot
            drawCircle(
                color = circleColor,
                radius = circleRadius * 0.15f,
                center = Offset(centerX, centerY)
            )
        }
    }
}

/**
 * Process FFT data into normalized heights for visualization
 */
private fun processFftData(fftData: ByteArray?, barCount: Int): List<Float> {
    if (fftData == null || fftData.isEmpty()) {
        return List(barCount) { 0f }
    }
    
    // FFT data is complex numbers (real, imaginary pairs)
    // We only need the magnitude, and only the first half (second half is mirror)
    val magnitudes = mutableListOf<Float>()
    val halfSize = fftData.size / 2
    
    // Skip first pair (DC component)
    for (i in 2 until halfSize step 2) {
        val real = fftData[i].toFloat()
        val imaginary = fftData[i + 1].toFloat()
        val magnitude = sqrt(real * real + imaginary * imaginary)
        magnitudes.add(magnitude)
    }
    
    // Group frequencies into bars
    val heights = mutableListOf<Float>()
    val samplesPerBar = magnitudes.size / barCount
    
    for (i in 0 until barCount) {
        val startIdx = i * samplesPerBar
        val endIdx = minOf(startIdx + samplesPerBar, magnitudes.size)
        
        if (startIdx < magnitudes.size) {
            // Average the magnitudes in this frequency range
            val avg = magnitudes.subList(startIdx, endIdx).average().toFloat()
            // Convert to dB and normalize
            val db = 20 * log10(avg + 1f)
            val normalized = (db / 80f).coerceIn(0f, 1f)
            heights.add(normalized)
        } else {
            heights.add(0f)
        }
    }
    
    return heights
}