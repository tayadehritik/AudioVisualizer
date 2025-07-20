# Build Performance Monitoring

## Build Time Target
**Maximum acceptable build time: 30 seconds**

## Current Build Status
- **Clean Build**: ~16 seconds ✅
- **Incremental Build**: ~3-5 seconds ✅

## Build Time History
| Date | Build Type | Time | Status |
|------|------------|------|--------|
| 2025-07-20 | Clean Build | 16s | ✅ PASS |
| 2025-07-20 | Incremental | 3s | ✅ PASS |

## Optimization Tips

### 1. Gradle Configuration
```groovy
// In gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
kotlin.incremental=true
```

### 2. Build Monitoring Script
Create `check-build-time.sh` in project root:

```bash
#!/bin/bash
# Build time monitoring script

MAX_TIME=30
START_TIME=$(date +%s)

./gradlew assembleDebug

END_TIME=$(date +%s)
BUILD_TIME=$((END_TIME - START_TIME))

echo "Build completed in ${BUILD_TIME} seconds"

if [ $BUILD_TIME -gt $MAX_TIME ]; then
    echo "⚠️  WARNING: Build time exceeded ${MAX_TIME}s threshold!"
    echo "Consider optimizing your build configuration."
    exit 1
else
    echo "✅ Build time is within acceptable range."
fi
```

### 3. Common Performance Issues

#### Slow Dependencies
- Keep dependencies up to date
- Remove unused dependencies
- Use implementation instead of api

#### Resource Processing
- Optimize image resources
- Use WebP format for images
- Enable resource shrinking in release builds

#### Kotlin Compilation
- Enable Kotlin incremental compilation
- Use Kotlin compiler daemon
- Avoid excessive use of inline functions

### 4. Monitoring Commands

```bash
# Profile build with detailed timing
./gradlew assembleDebug --profile

# Show build scan for detailed analysis
./gradlew assembleDebug --scan

# Clean build timing
./gradlew clean assembleDebug
```

### 5. Quick Performance Checks

Run these commands to monitor build performance:

```bash
# Check last 5 build times
grep "BUILD SUCCESSFUL" build/reports/profile/*.html | tail -5

# Monitor continuous builds
watch -n 1 './gradlew assembleDebug --quiet && echo "Build OK"'
```

## Action Items if Build Exceeds 30s

1. **Check for new dependencies** - Remove or optimize heavy libraries
2. **Review code generation** - Minimize annotation processors
3. **Analyze build profile** - Identify bottlenecks with --profile
4. **Clear caches** - `./gradlew clean buildCache`
5. **Update Gradle** - Ensure using latest stable version

## Notification Setup

To get notified when builds exceed 30s, add this to your `~/.zshrc` or `~/.bashrc`:

```bash
function gradle-timed() {
    START_TIME=$(date +%s)
    ./gradlew "$@"
    END_TIME=$(date +%s)
    BUILD_TIME=$((END_TIME - START_TIME))
    
    if [ $BUILD_TIME -gt 30 ]; then
        osascript -e "display notification \"Build took ${BUILD_TIME}s (> 30s limit)\" with title \"Slow Build Warning\" sound name \"Glass\""
    fi
}
```

Then use: `gradle-timed assembleDebug`

---

**Note**: Update this file whenever build configuration changes significantly.