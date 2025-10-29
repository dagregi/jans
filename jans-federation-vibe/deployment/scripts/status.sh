#!/bin/bash

# Status script for Jans Federation Vibe
# Shows status of federation entities
#
# Usage: ./status.sh [node_name]
#        ./status.sh              # Shows all running nodes
#        ./status.sh node1        # Shows specific node

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Get node name from argument (if provided)
NODE_NAME="$1"

echo "========================================="
echo "Jans Federation Vibe Status"
echo "========================================="
echo ""

# Function to show status for a specific node
show_node_status() {
    local node_name=$1
    local pid_file="$PROJECT_DIR/.federation-${node_name}.pid"
    
    if [ ! -f "$pid_file" ]; then
        return 1
    fi
    
    local pid=$(cat "$pid_file")
    
    if ! ps -p $pid > /dev/null 2>&1; then
        return 1
    fi
    
    # Derive port from node name
    local port=8080
    if [[ $node_name =~ ^node([0-9]+)$ ]]; then
        local node_num="${BASH_REMATCH[1]}"
        port=$((8080 + node_num - 1))
    fi
    
    echo "Node: $node_name"
    echo "  Status: ✅ RUNNING"
    echo "  PID: $pid"
    echo "  Port: $port"
    echo "  Entity ID: https://${node_name}.example.com"
    echo "  URL: http://localhost:$port"
    
    # Get process info
    local proc_info=$(ps -p $pid -o pid,ppid,user,%cpu,%mem,etime | tail -n 1)
    local cpu=$(echo $proc_info | awk '{print $4}')
    local mem=$(echo $proc_info | awk '{print $5}')
    local uptime=$(echo $proc_info | awk '{print $6}')
    
    echo "  CPU: ${cpu}%"
    echo "  Memory: ${mem}%"
    echo "  Uptime: $uptime"
    
    # Check endpoint health
    if curl -s http://localhost:$port/.well-known/openid-federation > /dev/null 2>&1; then
        echo "  Endpoints: ✅ Healthy"
        
        # Try to get subordinate count
        if curl -s http://localhost:$port/manage/subordinates > /dev/null 2>&1; then
            local subordinates=$(curl -s http://localhost:$port/manage/subordinates | grep -o '"entityId"' | wc -l | tr -d ' ')
            echo "  Subordinates: $subordinates"
        fi
    else
        echo "  Endpoints: ⚠️  Not responding"
    fi
    
    echo ""
    return 0
}

# If specific node requested
if [ -n "$NODE_NAME" ]; then
    if show_node_status "$NODE_NAME"; then
        echo "Commands:"
        echo "  Stop: ./deployment/scripts/stop.sh $NODE_NAME"
        echo "  Logs: tail -f /tmp/federation-${NODE_NAME}.log"
        echo "========================================="
        exit 0
    else
        echo "Status: ❌ NOT RUNNING"
        echo "Entity '$NODE_NAME' is not running"
        echo ""
        echo "To start: ./deployment/scripts/start.sh $NODE_NAME"
        exit 1
    fi
fi

# Show all running nodes
echo "Running Federation Entities:"
echo ""

FOUND=0
for pidfile in "$PROJECT_DIR"/.federation-*.pid; do
    if [ -f "$pidfile" ]; then
        node_name=$(basename "$pidfile" | sed 's/\.federation-//;s/\.pid//')
        if show_node_status "$node_name"; then
            FOUND=$((FOUND + 1))
        fi
    fi
done

if [ $FOUND -eq 0 ]; then
    echo "No federation entities are currently running."
    echo ""
    echo "To start an entity:"
    echo "  ./deployment/scripts/start.sh node1  # Port 8080"
    echo "  ./deployment/scripts/start.sh node2  # Port 8081"
    echo "  ./deployment/scripts/start.sh node3  # Port 8082"
else
    echo "Total running entities: $FOUND"
    echo ""
    echo "Commands:"
    echo "  Stop all: ./deployment/scripts/stop.sh node1 && ./deployment/scripts/stop.sh node2 && ..."
    echo "  Stop one: ./deployment/scripts/stop.sh <node_name>"
fi

echo "========================================="
