package com.example.audiovisualizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.audiovisualizer.presentation.navigation.AppNavigation
import com.example.audiovisualizer.ui.theme.AudioVisualizerTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioVisualizerTheme {
                AppNavigation()
            }
        }
    }
}
