#!/bin/bash

# Stop all entities from Appendix A test scenario

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "========================================="
echo "Stopping Appendix A Federation Entities"
echo "========================================="
echo ""

# Array of entities to stop
ENTITIES=("eduGAIN" "SWAMID" "UMU" "op-umu" "LIGO")

for entity in "${ENTITIES[@]}"; do
    "$SCRIPT_DIR/stop.sh" "$entity" 2>&1 | grep -E "Stopped|not running" || true
done

echo ""
echo "========================================="
echo "✅ All Appendix A Entities Stopped"
echo "========================================="

