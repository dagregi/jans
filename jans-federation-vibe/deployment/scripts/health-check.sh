#!/bin/bash

# Health check script for Jans Federation Vibe
# This script checks the health of the federation application

set -e

echo "Checking Jans Federation Vibe health..."

# Check if the application is running
if curl -f http://localhost:8080/federation/metadata > /dev/null 2>&1; then
    echo "✅ Application is healthy!"
    echo ""
    echo "🌐 Application URL: http://localhost:8080"
    echo "📋 API Documentation: http://localhost:8080/federation"
    echo "🔍 Health Check: http://localhost:8080/federation/metadata"
    echo ""
    
    # Test specific endpoints
    echo "Testing API endpoints..."
    
    # Test federation metadata
    if curl -f http://localhost:8080/federation/metadata > /dev/null 2>&1; then
        echo "✅ Federation metadata endpoint is working"
    else
        echo "❌ Federation metadata endpoint is not responding"
    fi
    
    # Test entity configuration endpoint
    if curl -f "http://localhost:8080/.well-known/openid-federation?iss=https://example.com" > /dev/null 2>&1; then
        echo "✅ Entity configuration endpoint is working"
    else
        echo "❌ Entity configuration endpoint is not responding"
    fi
    
    # Test trust mark issuers endpoint
    if curl -f http://localhost:8080/federation/trust-mark-issuers > /dev/null 2>&1; then
        echo "✅ Trust mark issuers endpoint is working"
    else
        echo "❌ Trust mark issuers endpoint is not responding"
    fi
    
    # Test database health endpoint
    if curl -f http://localhost:8080/database/health > /dev/null 2>&1; then
        echo "✅ Database health endpoint is working"
    else
        echo "❌ Database health endpoint is not responding"
    fi
    
    # Test database statistics endpoint
    if curl -f http://localhost:8080/database/stats > /dev/null 2>&1; then
        echo "✅ Database statistics endpoint is working"
    else
        echo "❌ Database statistics endpoint is not responding"
    fi
    
else
    echo "❌ Application is not healthy!"
    echo ""
    echo "Check the logs with:"
    echo "  docker-compose logs federation"
    echo ""
    echo "Or restart the application with:"
    echo "  ./deployment/scripts/start.sh"
    exit 1
fi
