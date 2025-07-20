package com.example.audiovisualizer.presentation.screens.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audiovisualizer.presentation.viewmodels.ToolsViewModel
import com.example.audiovisualizer.presentation.viewmodels.ViewModelFactory
import kotlin.math.roundToInt

/**
 * Tools/Settings screen for configuring visualizer parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ToolsViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizer Tools") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Bar Count Setting
            SettingCard(
                title = "Number of Bars",
                description = "Adjust the number of frequency bars in the visualizer"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bars: ${uiState.barCount}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        // Quick preset buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.updateBarCount(16) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("16", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.updateBarCount(32) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("32", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.updateBarCount(64) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("64", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = uiState.barCount.toFloat(),
                        onValueChange = { value ->
                            viewModel.updateBarCount(value.roundToInt())
                        },
                        valueRange = uiState.barCountRange.first.toFloat()..uiState.barCountRange.last.toFloat(),
                        steps = 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "Range: ${uiState.barCountRange.first} - ${uiState.barCountRange.last}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Animation Speed Setting
            SettingCard(
                title = "Animation Speed",
                description = "Control how quickly the bars animate"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speed: ${uiState.animationSpeed}ms",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = uiState.animationSpeed.toFloat(),
                        onValueChange = { value ->
                            viewModel.updateAnimationSpeed(value.roundToInt())
                        },
                        valueRange = uiState.animationSpeedRange.first.toFloat()..uiState.animationSpeedRange.last.toFloat(),
                        steps = 9, // 50ms increments
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Faster",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Slower",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Beat Detection Setting
            SettingCard(
                title = "Beat Detection Mode",
                description = "Enable energy-based beat detection for more responsive visualization"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Beat Detection",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = uiState.beatDetectionEnabled,
                            onCheckedChange = { viewModel.updateBeatDetectionEnabled(it) }
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = uiState.beatDetectionEnabled,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Beat Sensitivity
                            Column {
                                Text(
                                    text = "Sensitivity: ${String.format("%.1f", uiState.beatSensitivity)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = uiState.beatSensitivity,
                                    onValueChange = { viewModel.updateBeatSensitivity(it) },
                                    valueRange = uiState.beatSensitivityRange,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Higher = more sensitive to beats",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Beat Smoothing
                            Column {
                                Text(
                                    text = "Smoothing: ${String.format("%.2f", uiState.beatSmoothingFactor)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = uiState.beatSmoothingFactor,
                                    onValueChange = { viewModel.updateBeatSmoothingFactor(it) },
                                    valueRange = uiState.beatSmoothingRange,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Higher = smoother transitions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Coming Soon Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "More Settings Coming Soon",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Color customization\n• FFT window size\n• Export/Import settings\n• Advanced visualizer types",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Bottom padding for scroll
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Reusable setting card component.
 */
@Composable
fun SettingCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}