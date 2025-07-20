package com.example.audiovisualizer.presentation.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audiovisualizer.presentation.viewmodels.HomeViewModel
import com.example.audiovisualizer.presentation.viewmodels.ViewModelFactory
import com.tayadehritik.audiovisualizer.rememberAudioVisualizer
import com.tayadehritik.audiovisualizer.visualizers.AudioBarsVisualizer

/**
 * Home screen displaying the audio visualizer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTools: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState(initial = null)
    
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Visualizer") },
                actions = {
                    IconButton(onClick = onNavigateToTools) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            if (!hasRecordPermission) {
                // Permission request UI
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
                // Visualizer UI
                settings?.let { currentSettings ->
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
                        if (visualizerState != null) {
                            // Display the audio bars visualizer with settings from database
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                AudioBarsVisualizer(
                                    state = visualizerState,
                                    modifier = Modifier.fillMaxSize(),
                                    barCount = currentSettings.barCount,
                                    barColor = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Show active status and settings info
                            val isActive by visualizerState.isActive.collectAsState()
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isActive) "Visualizer Active" else "Visualizer Paused",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Bars: ${currentSettings.barCount}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    Text(
                                        text = "Play audio to see visualization",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
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
    }
}