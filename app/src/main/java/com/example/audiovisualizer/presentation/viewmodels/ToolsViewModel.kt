package com.example.audiovisualizer.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audiovisualizer.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Tools/Settings screen.
 * Manages visualizer configuration settings.
 */
class ToolsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()
    
    // Settings flow from repository
    val settings = settingsRepository.settings
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    barCount = settings.barCount,
                    primaryColor = settings.primaryColor,
                    animationSpeed = settings.animationSpeed,
                    sensitivity = settings.sensitivity,
                    smoothingEnabled = settings.smoothingEnabled,
                    beatDetectionEnabled = settings.beatDetectionEnabled,
                    beatSensitivity = settings.beatSensitivity,
                    beatSmoothingFactor = settings.beatSmoothingFactor,
                    isLoading = false
                )
            }
        }
    }
    
    fun updateBarCount(barCount: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(barCount = barCount)
            settingsRepository.updateBarCount(barCount)
        }
    }
    
    fun updateAnimationSpeed(speed: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(animationSpeed = speed)
            settingsRepository.updateAnimationSpeed(speed)
        }
    }
    
    fun updatePrimaryColor(color: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(primaryColor = color)
            settingsRepository.updatePrimaryColor(color)
        }
    }
    
    fun updateBeatDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(beatDetectionEnabled = enabled)
            settingsRepository.updateBeatDetectionEnabled(enabled)
        }
    }
    
    fun updateBeatSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(beatSensitivity = sensitivity)
            settingsRepository.updateBeatSensitivity(sensitivity)
        }
    }
    
    fun updateBeatSmoothingFactor(factor: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(beatSmoothingFactor = factor)
            settingsRepository.updateBeatSmoothingFactor(factor)
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            settingsRepository.resetToDefault()
        }
    }
}

/**
 * UI state for the Tools screen.
 */
data class ToolsUiState(
    val barCount: Int = 32,
    val barCountRange: IntRange = 8..256,
    val primaryColor: String = "#2196F3",
    val animationSpeed: Int = 100,
    val animationSpeedRange: IntRange = 50..500,
    val sensitivity: Float = 1.0f,
    val sensitivityRange: ClosedFloatingPointRange<Float> = 0.5f..2.0f,
    val smoothingEnabled: Boolean = true,
    val beatDetectionEnabled: Boolean = false,
    val beatSensitivity: Float = 0.7f,
    val beatSensitivityRange: ClosedFloatingPointRange<Float> = 0.3f..1.0f,
    val beatSmoothingFactor: Float = 0.8f,
    val beatSmoothingRange: ClosedFloatingPointRange<Float> = 0.5f..0.95f,
    val isLoading: Boolean = true
)