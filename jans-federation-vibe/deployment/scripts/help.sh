#!/bin/bash

# Help script for Jans Federation Vibe
# This script shows all available commands and their usage

echo "🚀 Jans Federation Vibe - Available Commands"
echo "=============================================="
echo ""

echo "📦 Build Commands:"
echo "  ./deployment/scripts/build.sh     - Build the Docker image"
echo "  ./deployment/scripts/clean.sh     - Clean up Docker resources"
echo ""

echo "🏃 Runtime Commands:"
echo "  ./deployment/scripts/start.sh     - Start the application"
echo "  ./deployment/scripts/stop.sh       - Stop the application"
echo "  ./deployment/scripts/health-check.sh - Check application health"
echo ""

echo "🛠️ Development Commands:"
echo "  ./deployment/scripts/dev.sh       - Start development environment"
echo "  ./deployment/scripts/help.sh      - Show this help message"
echo ""

echo "🐳 Docker Commands:"
echo "  docker-compose up -d              - Start all services"
echo "  docker-compose down               - Stop all services"
echo "  docker-compose logs -f federation - View application logs"
echo "  docker-compose ps                 - Show running containers"
echo ""

echo "🌐 Application URLs:"
echo "  http://localhost:8080              - Main application"
echo "  http://localhost:8080/federation   - API documentation"
echo "  http://localhost:8080/federation/metadata - Health check"
echo "  http://localhost:8081              - Database admin (dev mode)"
echo ""

echo "📋 API Endpoints:"
echo "  GET  /.well-known/openid-federation - Entity configuration"
echo "  GET  /federation/metadata           - Federation metadata"
echo "  POST /federation/validate-trust-chain - Trust chain validation"
echo "  GET  /federation/trust-marks         - Get trust marks"
echo "  POST /federation/issue-trust-mark   - Issue trust mark"
echo "  GET  /federation/trust-mark-issuers  - Get trust mark issuers"
echo "  GET  /database/health               - Database health check"
echo "  GET  /database/stats                - Database statistics"
echo "  POST /database/migrate              - Run database migrations"
echo ""

echo "🔧 Configuration:"
echo "  deployment/config/application.properties - Application settings"
echo "  deployment/config/nginx.conf              - Nginx configuration"
echo "  deployment/config/log4j2.xml              - Logging configuration"
echo ""

echo "📚 Documentation:"
echo "  deployment/README.md               - Complete deployment guide"
echo "  ../README.md                       - Project documentation"
echo ""

echo "🆘 Troubleshooting:"
echo "  docker-compose logs federation     - Check application logs"
echo "  docker-compose ps                  - Check container status"
echo "  ./deployment/scripts/health-check.sh - Test application health"
echo ""

echo "💡 Quick Start:"
echo "  1. ./deployment/scripts/build.sh"
echo "  2. ./deployment/scripts/start.sh"
echo "  3. ./deployment/scripts/health-check.sh"
echo ""

echo "For more information, see deployment/README.md"
