package com.example.audiovisualizer.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.audiovisualizer.data.database.AppDatabase
import com.example.audiovisualizer.data.repository.SettingsRepository

/**
 * Factory for creating ViewModels with dependencies.
 * This is a temporary solution until Hilt issues are resolved.
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    private val database by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    private val settingsRepository by lazy {
        SettingsRepository(database.visualizerSettingsDao())
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(settingsRepository) as T
            }
            modelClass.isAssignableFrom(ToolsViewModel::class.java) -> {
                ToolsViewModel(settingsRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}