#!/bin/bash

# Status script for Jans Federation Vibe
# This script shows the current status of the federation application

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
PID_FILE="$PROJECT_DIR/.federation.pid"

echo "========================================="
echo "Jans Federation Vibe Status"
echo "========================================="
echo ""

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "Status: ❌ NOT RUNNING"
    echo "Reason: PID file not found"
    echo ""
    
    # Check if process is actually running (without PID file)
    PIDS=$(ps aux | grep "jans-federation-vibe.*\.jar" | grep -v grep | awk '{print $2}')
    if [ -n "$PIDS" ]; then
        echo "⚠️  Warning: Found orphaned federation processes: $PIDS"
        echo "Run './deployment/scripts/stop.sh' to clean up"
    fi
    
    echo ""
    echo "To start: ./deployment/scripts/start.sh"
    exit 1
fi

# Read PID from file
PID=$(cat "$PID_FILE")

# Check if process is running
if ! ps -p $PID > /dev/null 2>&1; then
    echo "Status: ❌ NOT RUNNING"
    echo "Reason: Process $PID not found (stale PID file)"
    echo ""
    echo "To start: ./deployment/scripts/start.sh"
    exit 1
fi

echo "Status: ✅ RUNNING"
echo "PID: $PID"
echo ""

# Get process info
echo "Process Information:"
ps -p $PID -o pid,ppid,user,%cpu,%mem,etime,command | tail -n 1 | awk '{printf "  PID: %s\n  User: %s\n  CPU: %s%%\n  Memory: %s%%\n  Uptime: %s\n", $1, $3, $4, $5, $6}'
echo ""

# Check application health
echo "Application Health:"
if curl -s http://localhost:8080/database/health > /dev/null 2>&1; then
    HEALTH_RESPONSE=$(curl -s http://localhost:8080/database/health)
    echo "  Database: ✅ Connected"
    
    # Get stats
    if curl -s http://localhost:8080/database/stats > /dev/null 2>&1; then
        STATS=$(curl -s http://localhost:8080/database/stats)
        TOTAL_ENTITIES=$(echo $STATS | grep -o '"total_entities":[0-9]*' | cut -d':' -f2)
        TRUST_MARKS=$(echo $STATS | grep -o '"active_trust_marks":[0-9]*' | cut -d':' -f2)
        echo "  Total Entities: $TOTAL_ENTITIES"
        echo "  Active Trust Marks: $TRUST_MARKS"
    fi
else
    echo "  Database: ⚠️  Not responding"
fi

echo ""

# Check endpoints
echo "Endpoint Status:"
ENDPOINTS=(
    "http://localhost:8080/.well-known/openid-federation?iss=https://op.example.com|Entity Configuration"
    "http://localhost:8080/federation/metadata|Federation Metadata"
    "http://localhost:8080/federation/trust-marks|Trust Marks"
    "http://localhost:8080/federation/jwks|JWKS"
    "http://localhost:8080/database/health|Health Check"
)

for ENDPOINT_INFO in "${ENDPOINTS[@]}"; do
    URL=$(echo $ENDPOINT_INFO | cut -d'|' -f1)
    NAME=$(echo $ENDPOINT_INFO | cut -d'|' -f2)
    
    if curl -s -f "$URL" > /dev/null 2>&1; then
        echo "  ✅ $NAME"
    else
        echo "  ❌ $NAME"
    fi
done

echo ""
echo "========================================="
echo "URLs:"
echo "  Application: http://localhost:8080"
echo "  Metadata: http://localhost:8080/federation/metadata"
echo "  Health: http://localhost:8080/database/health"
echo ""
echo "Commands:"
echo "  Logs:  tail -f /tmp/federation-server.log"
echo "  Test:  mvn test"
echo "  Stop:  ./deployment/scripts/stop.sh"
echo "========================================="

