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
import com.tayadehritik.audiovisualizer.rememberAudioVisualizer
import com.tayadehritik.audiovisualizer.visualizers.AudioBarsVisualizer

@Composable
fun HomePage(
    modifier: Modifier = Modifier
) {
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
        modifier = modifier.fillMaxSize(),
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
            // Use the new Compose-first API
            val visualizerState = rememberAudioVisualizer(
                audioSessionId = 0,
                enabled = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Audio Visualizer",
                    style = MaterialTheme.typography.headlineLarge,
                )

                if (visualizerState != null) {
                    // Display the audio bars visualizer
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        AudioBarsVisualizer(
                            state = visualizerState,
                            modifier = Modifier.fillMaxSize(),
                            barCount = 32,
                            barColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Show active status
                    val isActive by visualizerState.isActive.collectAsState()
                    Text(
                        text = if (isActive) "Visualizer Active" else "Visualizer Paused",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Failed to initialize audio visualizer. Make sure audio is playing on your device.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
