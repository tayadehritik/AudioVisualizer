# Audio Visualizers

This package contains various audio visualization components for Android Compose applications.

## Available Visualizers

### 1. AudioBarsVisualizer
- Simple vertical bars representing frequency ranges
- Customizable bar count, colors, and spacing
- Smooth animations with configurable duration

### 2. CircularBarsVisualizer
- Bars arranged in a circular pattern
- Configurable inner/outer radius
- Optional rotation animation
- Individual bar width control

### 3. CircularWaveVisualizer
- Smooth wave pattern in circular form
- Continuous flowing effect
- Option to fill the wave area
- Heavy smoothing for fluid motion

### 4. OsuStyleCircularVisualizer
- Inspired by osu! game's logo visualizer
- Multiple rounds of bars (spiral effect)
- Spring animations for bouncy response
- Decay effect when audio stops
- Gradient coloring option
- Logarithmic frequency distribution

## Usage Example

```kotlin
@Composable
fun MyAudioVisualizerScreen() {
    val visualizerState = rememberAudioVisualizer()
    
    OsuStyleCircularVisualizer(
        state = visualizerState,
        barCount = 160,
        visualizerRounds = 4,
        barColor = MaterialTheme.colorScheme.primary,
        useGradient = true
    )
}
```

## Implementation Details

All visualizers:
- Process FFT (Fast Fourier Transform) data from Android's Visualizer API
- Convert complex frequency data to magnitudes
- Apply logarithmic scaling for better visual representation
- Smooth transitions between frames
- Support customizable colors and sizes

The circular visualizers use trigonometric functions to position elements radially and create engaging visual effects synchronized with audio.