# DreamServiceCompat - ComposeView Support for DreamService

## Problem
When using `ComposeView` in a `DreamService`, you encounter the error:
```
java.lang.IllegalStateException: ViewTreeLifecycleOwner not found from androidx.compose.ui.platform.ComposeView
```

This happens because ComposeView requires three "Owner" interfaces that are not provided by DreamService:
- `LifecycleOwner`
- `ViewModelStoreOwner`
- `SavedStateRegistryOwner`

## Solution
The `DreamServiceCompat` class extends `DreamService` and implements all three required interfaces, providing proper lifecycle management for Compose UI.

### Key Features:
1. **Lifecycle Management**: Properly transitions through lifecycle states based on DreamService callbacks
2. **ViewModelStore Support**: Allows use of ViewModels within the dream UI
3. **SavedState Support**: Enables state restoration capabilities
4. **Automatic ViewTree Setup**: The `setContent()` method automatically configures all required ViewTree owners

### Usage:
```kotlin
class MyDreamService : DreamServiceCompat() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // Configure your dream
        isInteractive = false
        isFullscreen = true
        
        // Set Compose content
        setContent {
            // Your Compose UI here
            MyDreamUI()
        }
    }
}
```

### Implementation Details:
- Uses `LifecycleRegistry` to manage lifecycle state transitions
- Creates and manages a `ViewModelStore` instance
- Initializes `SavedStateRegistryController` for state persistence
- Automatically sets up ViewTree owners when `setContent()` is called
- Uses `ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool` for proper cleanup

### Lifecycle Flow:
1. `onCreate()` → CREATED state
2. `onDreamingStarted()` → STARTED → RESUMED state
3. `onDreamingStopped()` → STARTED → CREATED state
4. `onDestroy()` → DESTROYED state (clears ViewModelStore)

This solution ensures that ComposeView works correctly in DreamService without encountering ViewTreeLifecycleOwner errors.