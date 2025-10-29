#!/bin/bash

# Start all entities for Appendix A test scenario
# 
# This script starts the exact federation hierarchy described in
# OpenID Federation 1.0 Appendix A:
# 
#   eduGAIN (Trust Anchor)
#     └── SWAMID (Intermediate)
#         └── UMU (Organization)
#             └── OP.UMU (OpenID Provider)
#   
#   Plus: LIGO (Relying Party)

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "========================================="
echo "Starting Appendix A Federation Entities"
echo "========================================="
echo ""
echo "Per OpenID Federation 1.0 Appendix A:"
echo "  Example: OpenID Provider Information Discovery"
echo ""
echo "Entity Hierarchy:"
echo "  eduGAIN (https://edugain.geant.org) - Trust Anchor - Port 8080"
echo "    └── SWAMID (https://swamid.se) - Intermediate - Port 8081"
echo "        └── UMU (https://umu.se) - Organization - Port 8082"
echo "            └── OP.UMU (https://op.umu.se) - OpenID Provider - Port 8083"
echo ""
echo "  LIGO (https://ligo.example.org) - Relying Party - Port 8084"
echo ""
echo "========================================="
echo ""

# Array of entities to start (in order)
ENTITIES=("eduGAIN" "SWAMID" "UMU" "op-umu" "LIGO")

for entity in "${ENTITIES[@]}"; do
    echo "Starting $entity..."
    "$SCRIPT_DIR/start.sh" "$entity" > /dev/null 2>&1
    sleep 1
    echo "  ✓ $entity started"
done

echo ""
echo "========================================="
echo "✅ All Appendix A Entities Started"
echo "========================================="
echo ""

# Show status
"$SCRIPT_DIR/status.sh"

echo ""
echo "Next steps:"
echo "  1. Run Appendix A integration test:"
echo "     mvn test -Dtest=AppendixAIntegrationTest"
echo ""
echo "  2. Stop all entities:"
echo "     ./deployment/scripts/stop-appendix-a.sh"
echo ""

