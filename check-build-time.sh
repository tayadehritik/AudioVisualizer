#!/bin/bash
# AudioVisualizer Build Time Monitor
# Alerts if build time exceeds 30 seconds

MAX_TIME=30
BUILD_TYPE=${1:-assembleDebug}

echo "🔨 Starting build monitoring..."
echo "Maximum allowed time: ${MAX_TIME}s"
echo "Build type: ${BUILD_TYPE}"
echo "================================"

START_TIME=$(date +%s)

# Run the build
./gradlew $BUILD_TYPE

BUILD_SUCCESS=$?
END_TIME=$(date +%s)
BUILD_TIME=$((END_TIME - START_TIME))

echo "================================"
echo "Build completed in ${BUILD_TIME} seconds"

# Check build success
if [ $BUILD_SUCCESS -ne 0 ]; then
    echo "❌ Build FAILED"
    exit 1
fi

# Check build time
if [ $BUILD_TIME -gt $MAX_TIME ]; then
    echo "⚠️  WARNING: Build time exceeded ${MAX_TIME}s threshold!"
    echo "⚠️  Actual time: ${BUILD_TIME}s (${BUILD_TIME - MAX_TIME}s over limit)"
    
    # macOS notification
    if [[ "$OSTYPE" == "darwin"* ]]; then
        osascript -e "display notification \"Build took ${BUILD_TIME}s (exceeded ${MAX_TIME}s limit)\" with title \"⚠️ Slow Build Warning\" subtitle \"AudioVisualizer\" sound name \"Glass\""
    fi
    
    # Linux notification (requires notify-send)
    if [[ "$OSTYPE" == "linux-gnu"* ]] && command -v notify-send &> /dev/null; then
        notify-send "⚠️ Slow Build Warning" "Build took ${BUILD_TIME}s (exceeded ${MAX_TIME}s limit)" --urgency=critical
    fi
    
    echo ""
    echo "Suggestions to improve build time:"
    echo "- Run './gradlew assembleDebug --profile' to analyze"
    echo "- Check BUILD_PERFORMANCE.md for optimization tips"
    echo "- Clear cache with './gradlew clean'"
    exit 1
else
    echo "✅ Build time is within acceptable range (${BUILD_TIME}s < ${MAX_TIME}s)"
    
    # Optional: Success notification for builds close to limit
    if [ $BUILD_TIME -gt 25 ]; then
        echo "⚡ Note: Build time is approaching the ${MAX_TIME}s limit"
    fi
fi

# Save build time to history
echo "$(date +"%Y-%m-%d %H:%M:%S") | ${BUILD_TYPE} | ${BUILD_TIME}s | $([ $BUILD_TIME -gt $MAX_TIME ] && echo "SLOW" || echo "OK")" >> build-times.log

echo ""
echo "📊 Recent build times:"
tail -5 build-times.log 2>/dev/null || echo "No history available yet."