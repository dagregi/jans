#!/bin/bash

# Start script for Jans Federation Vibe
# This script builds and starts the federation application

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
PID_FILE="$PROJECT_DIR/.federation.pid"
LOG_FILE="/tmp/federation-server.log"

echo "========================================="
echo "Starting Jans Federation Vibe"
echo "========================================="
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH"
    echo "Please install Java 11 or higher and try again."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ Error: Java 11 or higher is required"
    echo "Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

echo "✓ Java version: $(java -version 2>&1 | head -n 1 | cut -d'"' -f2)"

# Check if already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "⚠️  Federation server is already running (PID: $PID)"
        echo "Use './deployment/scripts/stop.sh' to stop it first"
        exit 1
    else
        echo "⚠️  Removing stale PID file"
        rm -f "$PID_FILE"
    fi
fi

# Build the application
echo ""
echo "Building application..."
cd "$PROJECT_DIR"

if ! mvn clean package -DskipTests > /dev/null 2>&1; then
    echo "❌ Build failed. Running with verbose output:"
    mvn clean package -DskipTests
    exit 1
fi

echo "✓ Build successful"

# Find the executable JAR
JAR_FILE="$PROJECT_DIR/target/jans-federation-vibe-1.13.0-executable.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: Executable JAR not found at $JAR_FILE"
    exit 1
fi

echo "✓ Executable JAR found: $(basename $JAR_FILE)"

# Start the application
echo ""
echo "Starting federation server..."

java -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

# Save PID
echo $SERVER_PID > "$PID_FILE"

echo "✓ Server started (PID: $SERVER_PID)"
echo "✓ Logs: $LOG_FILE"

# Wait for server to start
echo ""
echo "Waiting for server to be ready..."
MAX_WAIT=30
WAIT_COUNT=0

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if curl -s http://localhost:8080/federation/metadata > /dev/null 2>&1; then
        echo "✓ Server is ready!"
        break
    fi
    
    # Check if process is still running
    if ! ps -p $SERVER_PID > /dev/null 2>&1; then
        echo "❌ Server process died unexpectedly!"
        echo ""
        echo "Last 20 lines of log:"
        tail -20 "$LOG_FILE"
        rm -f "$PID_FILE"
        exit 1
    fi
    
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
    echo -n "."
done

echo ""

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    echo "❌ Server failed to start within ${MAX_WAIT} seconds"
    echo ""
    echo "Last 20 lines of log:"
    tail -20 "$LOG_FILE"
    kill $SERVER_PID 2>/dev/null || true
    rm -f "$PID_FILE"
    exit 1
fi

# Display success message
echo ""
echo "========================================="
echo "✅ Jans Federation Vibe Started!"
echo "========================================="
echo ""
echo "🌐 Application URL: http://localhost:8080"
echo "📋 Federation Metadata: http://localhost:8080/federation/metadata"
echo "🔍 Health Check: http://localhost:8080/database/health"
echo "📊 Database Stats: http://localhost:8080/database/stats"
echo ""
echo "📝 PID File: $PID_FILE"
echo "📄 Log File: $LOG_FILE"
echo ""
echo "Commands:"
echo "  Status:  ./deployment/scripts/status.sh"
echo "  Stop:    ./deployment/scripts/stop.sh"
echo "  Logs:    tail -f $LOG_FILE"
echo "  Test:    mvn test"
echo ""
