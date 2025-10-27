#!/bin/bash

# Clean script for Jans Federation Vibe
# This script cleans up Docker containers, images, and volumes

set -e

echo "Cleaning up Jans Federation Vibe..."

# Stop and remove containers
echo "Stopping and removing containers..."
docker-compose down -v

# Remove the application image
echo "Removing Docker image..."
docker rmi jans-federation-vibe:latest 2>/dev/null || true

# Remove unused images
echo "Removing unused Docker images..."
docker image prune -f

# Remove unused volumes
echo "Removing unused Docker volumes..."
docker volume prune -f

# Remove unused networks
echo "Removing unused Docker networks..."
docker network prune -f

echo "✅ Cleanup completed!"
echo ""
echo "To completely reset the environment:"
echo "  docker system prune -a"
echo ""
echo "To rebuild the application:"
echo "  ./deployment/scripts/build.sh"
