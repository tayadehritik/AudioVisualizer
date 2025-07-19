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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(backgroundColor, size = size)
        
        if (animatedHeights.isNotEmpty()) {
            val barWidth = size.width / barCount
            val barSpacing = with(density) { 2.dp.toPx() }
            
            animatedHeights.forEachIndexed { index, normalizedHeight ->
                val animatedHeight = normalizedHeight * size.height * 0.8f
                val left = index * barWidth + barSpacing / 2
                val barActualWidth = barWidth - barSpacing
                val top = size.height - animatedHeight
                
                drawRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barActualWidth, animatedHeight)
                )
            }
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