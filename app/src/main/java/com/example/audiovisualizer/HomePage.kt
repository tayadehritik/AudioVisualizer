package com.example.audiovisualizer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tayadehritik.audiovisualizer.AudioVisualizer

@Composable
fun HomePage() {
    val context = LocalContext.current
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasRecordPermission = isGranted
        }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasRecordPermission) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    text = "Audio Recording Permission Required",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "This app needs access to your microphone to visualize audio.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )

                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                ) {
                    Text("Grant Permission")
                }
            }
        } else {
            val visualizer = remember {
                try {
                    AudioVisualizer(0).also {
                        Log.d("HomePage", "AudioVisualizer created successfully")
                    }
                } catch (e: Exception) {
                    Log.e("HomePage", "Failed to create AudioVisualizer", e)
                    null
                }
            }

            DisposableEffect(visualizer) {
                onDispose {
                    visualizer?.release()
                    Log.d("HomePage", "AudioVisualizer released")
                }
            }

            // Collect FFT data from the Flow as State
            val fftData by visualizer?.fftDataFlow?.collectAsState() ?: remember {
                mutableStateOf(
                    null
                )
            }

            // Debug logging
            LaunchedEffect(fftData) {
                Log.d("HomePage", "FFT data in UI: ${fftData?.size ?: "null"}")
            }

            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Welcome to Audio Visualizer",
                    style = MaterialTheme.typography.headlineLarge,
                )

                Text(
                    text = "Permission granted! Ready to visualize audio.",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = "FFT Data: ${fftData?.size ?: 0} bytes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )

                fftData?.let { data ->
                    Text(
                        text = "First 10 values: ${data.take(10).joinToString { it.toString() }}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Show more detailed data
                    Text(
                        text = "Data sample: ${data.take(5).map { it.toInt() }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
