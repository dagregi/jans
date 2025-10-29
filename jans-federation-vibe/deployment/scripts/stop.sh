#!/bin/bash

# Stop script for Jans Federation Vibe
# This script stops a specific federation entity (node)
#
# Usage: ./stop.sh [node_name]
# Example: ./stop.sh node1
#          ./stop.sh node2

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Get node name from argument (default: node1)
NODE_NAME="${1:-node1}"

PID_FILE="$PROJECT_DIR/.federation-${NODE_NAME}.pid"

echo "========================================="
echo "Stopping Federation Entity '$NODE_NAME'"
echo "========================================="
echo ""

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "⚠️  PID file not found for '$NODE_NAME'"
    echo "Entity may not be running."
    echo ""
    echo "Active federation entities:"
    for pidfile in "$PROJECT_DIR"/.federation-*.pid; do
        if [ -f "$pidfile" ]; then
            name=$(basename "$pidfile" | sed 's/.federation-//;s/.pid//')
            pid=$(cat "$pidfile")
            if ps -p $pid > /dev/null 2>&1; then
                echo "  - $name (PID: $pid)"
            fi
        fi
    done
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
echo "Stopping entity '$NODE_NAME' (PID: $PID)..."

kill $PID 2>/dev/null

# Wait for process to stop
WAIT_COUNT=0
MAX_WAIT=10

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✓ Entity '$NODE_NAME' stopped successfully"
        rm -f "$PID_FILE"
        echo ""
        echo "========================================="
        echo "✅ Federation Entity '$NODE_NAME' Stopped"
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
    echo "✓ Entity '$NODE_NAME' force-stopped"
    rm -f "$PID_FILE"
else
    echo "❌ Failed to stop process $PID"
    exit 1
fi

echo ""
echo "========================================="
echo "✅ Federation Entity '$NODE_NAME' Stopped"
echo "========================================="
