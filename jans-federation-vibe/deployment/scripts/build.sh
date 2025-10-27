#!/bin/bash

# Build script for Jans Federation Vibe
# This script builds the Docker image for the federation application

set -e

echo "Building Jans Federation Vibe Docker image..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Build the Docker image
docker build -f deployment/Dockerfile -t jans-federation-vibe:latest .

echo "Docker image built successfully!"
echo "Image name: jans-federation-vibe:latest"
echo ""
echo "To run the application:"
echo "  docker run -p 8080:8080 jans-federation-vibe:latest"
echo ""
echo "Or use docker-compose:"
echo "  docker-compose up -d"
