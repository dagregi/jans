#!/bin/bash

# Stop script for Jans Federation Vibe
# This script stops the federation application

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
PID_FILE="$PROJECT_DIR/.federation.pid"

echo "========================================="
echo "Stopping Jans Federation Vibe"
echo "========================================="
echo ""

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "⚠️  PID file not found. Server may not be running."
    
    # Try to find and kill the process anyway
    PIDS=$(ps aux | grep "jans-federation-vibe.*\.jar" | grep -v grep | awk '{print $2}')
    
    if [ -n "$PIDS" ]; then
        echo "Found running federation processes: $PIDS"
        echo "Stopping them..."
        for PID in $PIDS; do
            kill $PID 2>/dev/null && echo "✓ Stopped process $PID" || echo "⚠️  Failed to stop process $PID"
        done
    else
        echo "No federation processes found running."
    fi
    exit 0
fi

# Read PID from file
PID=$(cat "$PID_FILE")

# Check if process is running
if ! ps -p $PID > /dev/null 2>&1; then
    echo "⚠️  Process $PID is not running (stale PID file)"
    rm -f "$PID_FILE"
    exit 0
fi

# Stop the process
echo "Stopping federation server (PID: $PID)..."

kill $PID 2>/dev/null

# Wait for process to stop
WAIT_COUNT=0
MAX_WAIT=10

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✓ Server stopped successfully"
        rm -f "$PID_FILE"
        echo ""
        echo "========================================="
        echo "✅ Jans Federation Vibe Stopped"
        echo "========================================="
        exit 0
    fi
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
    echo -n "."
done

echo ""
echo "⚠️  Process did not stop gracefully, forcing shutdown..."
kill -9 $PID 2>/dev/null

if ! ps -p $PID > /dev/null 2>&1; then
    echo "✓ Server force-stopped"
    rm -f "$PID_FILE"
else
    echo "❌ Failed to stop server process $PID"
    exit 1
fi

echo ""
echo "========================================="
echo "✅ Jans Federation Vibe Stopped"
echo "========================================="
