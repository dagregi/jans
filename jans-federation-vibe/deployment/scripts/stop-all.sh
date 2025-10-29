#!/bin/bash

# Stop all federation nodes
# This script stops all running federation entities

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "========================================="
echo "Stopping All Federation Entities"
echo "========================================="
echo ""

# Find all PID files
STOPPED_COUNT=0
for pidfile in "$PROJECT_DIR"/.federation-*.pid; do
    if [ -f "$pidfile" ]; then
        node_name=$(basename "$pidfile" | sed 's/\.federation-//;s/\.pid//')
        echo "Stopping $node_name..."
        "$SCRIPT_DIR/stop.sh" "$node_name" 2>&1 | grep -E "Stopping|Stopped"
        STOPPED_COUNT=$((STOPPED_COUNT + 1))
    fi
done

if [ $STOPPED_COUNT -eq 0 ]; then
    echo "No federation entities were running."
else
    echo ""
    echo "========================================="
    echo "✅ Stopped $STOPPED_COUNT Federation Entities"
    echo "========================================="
fi

