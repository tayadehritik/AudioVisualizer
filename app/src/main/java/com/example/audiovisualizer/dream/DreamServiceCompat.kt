package com.example.audiovisualizer.dream

import android.service.dreams.DreamService
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * A DreamService that properly supports Jetpack Compose by implementing all required owner interfaces.
 * This allows ComposeView to work correctly within a DreamService context.
 */
abstract class DreamServiceCompat : DreamService(), 
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    
    override val viewModelStore: ViewModelStore
        get() = store
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
    
    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
    
    override fun onDreamingStopped() {
        super.onDreamingStopped()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDetachedFromWindow()
    }
    
    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        super.onDestroy()
    }
    
    /**
     * Sets the content of the DreamService using Compose.
     * This method properly configures the ComposeView with all required owners.
     */
    protected fun setComposeContent(content: @Composable () -> Unit) {
        val composeView = ComposeView(this).apply {
            // Set all required ViewTree owners
            setViewTreeLifecycleOwner(this@DreamServiceCompat)
            setViewTreeViewModelStoreOwner(this@DreamServiceCompat)
            setViewTreeSavedStateRegistryOwner(this@DreamServiceCompat)
            
            // Set the appropriate composition strategy
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle)
            )
            
            setContent(content)
        }
        
        setContentView(composeView)
    }
}