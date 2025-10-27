#!/bin/bash

# Startup script for Jans Federation Vibe
# This script initializes the database and starts the application

set -e

echo "🚀 Starting Jans Federation Vibe..."

# Initialize database if needed
echo "🗄️ Checking database initialization..."
if [ -n "$FEDERATION_DATABASE_URL" ]; then
    echo "📊 Database configuration detected, running initialization..."
    /app/init-db.sh
else
    echo "⚠️ No database configuration found, running without database"
fi

# Start Tomcat
echo "🌐 Starting Tomcat application server..."
exec catalina.sh run
