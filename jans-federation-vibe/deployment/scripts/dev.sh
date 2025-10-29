#!/bin/bash

# Development script for Jans Federation Vibe
# This script sets up a development environment with hot reloading

set -e

echo "Setting up Jans Federation Vibe development environment..."

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven and try again."
    exit 1
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Build the application locally
echo "Building application locally..."
mvn clean package -DskipTests

# Start development services
echo "Starting development services..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

echo "✅ Development environment is ready!"
echo ""
echo "🌐 Application URL: http://localhost:8080"
echo "📋 API Documentation: http://localhost:8080/federation"
echo "🔍 Health Check: http://localhost:8080/federation/metadata"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f federation"
echo ""
echo "To rebuild and restart:"
echo "  mvn clean package -DskipTests && docker-compose restart federation"
echo ""
echo "To stop development environment:"
echo "  docker-compose down"

