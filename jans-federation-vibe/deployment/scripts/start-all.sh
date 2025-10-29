#!/bin/bash

# Start all federation nodes for testing
# This script starts node1 (Trust Anchor), node2, and node3

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "========================================="
echo "Starting All Federation Entities"
echo "========================================="
echo ""

# Array of nodes to start
NODES=("node1" "node2" "node3")

echo "Starting ${#NODES[@]} nodes..."
echo ""

# Start each node
for node in "${NODES[@]}"; do
    echo "Starting $node..."
    "$SCRIPT_DIR/start.sh" "$node"
    echo ""
done

echo "========================================="
echo "✅ All Federation Entities Started"
echo "========================================="
echo ""

# Show status
"$SCRIPT_DIR/status.sh"

