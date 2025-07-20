package com.example.audiovisualizer.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audiovisualizer.data.database.entities.VisualizerSettings
import com.example.audiovisualizer.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Manages visualizer state and settings.
 */
class HomeViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
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
                    animationSpeed = settings.animationSpeed
                )
            }
        }
    }
    
    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasAudioPermission = true)
    }
    
    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(hasAudioPermission = false)
    }
}

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val hasAudioPermission: Boolean = false,
    val isVisualizerActive: Boolean = false,
    val barCount: Int = 32,
    val primaryColor: String = "#2196F3",
    val animationSpeed: Int = 100,
    val errorMessage: String? = null
)