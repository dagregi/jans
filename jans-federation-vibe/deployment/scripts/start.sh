#!/bin/bash

# Start script for Jans Federation Vibe
# This script builds and starts a federation entity (node)
#
# Usage: ./start.sh [node_name]
# Example: ./start.sh node1
#          ./start.sh node2

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Get node name from argument (default: node1)
NODE_NAME="${1:-node1}"

# Derive port from node name
# Support both numbered nodes (node1, node2) and named entities (eduGAIN, SWAMID, etc.)
NODE_NAME_LOWER=$(echo "$NODE_NAME" | tr '[:upper:]' '[:lower:]')

case "$NODE_NAME_LOWER" in
    edugain) PORT=8080 ;;
    swamid) PORT=8081 ;;
    umu) PORT=8082 ;;
    op-umu|opumu) PORT=8083 ;;
    ligo) PORT=8084 ;;
    *)
        # Handle nodeN format
        if [[ $NODE_NAME =~ ^node([0-9]+)$ ]]; then
            NODE_NUM="${BASH_REMATCH[1]}"
            PORT=$((8080 + NODE_NUM - 1))
        else
            echo "❌ Error: Unknown node name: $NODE_NAME"
            echo "Supported: node1, node2, node3, ... or eduGAIN, SWAMID, UMU, op-umu, LIGO"
            exit 1
        fi
        ;;
esac

PID_FILE="$PROJECT_DIR/.federation-${NODE_NAME}.pid"
LOG_FILE="/tmp/federation-${NODE_NAME}.log"

echo "========================================="
echo "Starting Jans Federation Entity"
echo "========================================="
echo "Node Name: $NODE_NAME"
echo "Port: $PORT"
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

# Check if this node is already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "⚠️  Entity '$NODE_NAME' is already running (PID: $PID, Port: $PORT)"
        echo "Use './deployment/scripts/stop.sh $NODE_NAME' to stop it first"
        exit 1
    else
        echo "⚠️  Removing stale PID file for $NODE_NAME"
        rm -f "$PID_FILE"
    fi
fi

# Check if port is already in use
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "❌ Error: Port $PORT is already in use"
    echo "Another process is using this port. Please stop it first."
    lsof -i :$PORT | head -2
    exit 1
fi

# Build the application if needed
JAR_FILE="$PROJECT_DIR/target/jans-federation-vibe-1.13.0-executable.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo ""
    echo "Building application..."
    cd "$PROJECT_DIR"
    
    if ! mvn clean package -DskipTests > /dev/null 2>&1; then
        echo "❌ Build failed. Running with verbose output:"
        mvn clean package -DskipTests
        exit 1
    fi
    
    echo "✓ Build successful"
else
    echo "✓ Using existing build: $(basename $JAR_FILE)"
fi

# Start the application
echo ""
echo "Starting entity '$NODE_NAME' on port $PORT..."

# Set PORT environment variable and pass node name as argument
PORT=$PORT java -jar "$JAR_FILE" "$NODE_NAME" > "$LOG_FILE" 2>&1 &
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
    if curl -s http://localhost:$PORT/.well-known/openid-federation > /dev/null 2>&1; then
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
echo "✅ Federation Entity '$NODE_NAME' Started!"
echo "========================================="
echo ""
echo "📌 Node Name: $NODE_NAME"
echo "🆔 Entity ID: https://${NODE_NAME}.example.com"
echo "🌐 Base URL: http://localhost:$PORT"
echo "📋 Entity Config: http://localhost:$PORT/.well-known/openid-federation"
echo "🔧 Management API: http://localhost:$PORT/manage"
echo ""
echo "📝 PID: $SERVER_PID (saved to $PID_FILE)"
echo "📄 Logs: $LOG_FILE"
echo ""
echo "Commands:"
echo "  Status: ./deployment/scripts/status.sh $NODE_NAME"
echo "  Stop:   ./deployment/scripts/stop.sh $NODE_NAME"
echo "  Logs:   tail -f $LOG_FILE"
echo ""
echo "To start another entity:"
echo "  ./deployment/scripts/start.sh node2  # Will use port 8081"
echo "  ./deployment/scripts/start.sh node3  # Will use port 8082"
echo ""
