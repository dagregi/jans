#!/bin/bash

# Database setup script for Jans Federation Vibe
# This script sets up the database with tables and sample data for development

set -e

echo "🗄️ Setting up Jans Federation Vibe database..."

# Check if PostgreSQL is running
if ! pg_isready -h postgres -p 5432 -U federation > /dev/null 2>&1; then
    echo "❌ PostgreSQL is not running or not accessible"
    echo "Please start the database first:"
    echo "  docker-compose up -d postgres"
    exit 1
fi

echo "✅ PostgreSQL is running"

# Check if database exists
if psql -h postgres -U federation -d federation -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Database 'federation' exists"
else
    echo "❌ Database 'federation' does not exist"
    echo "Please create the database first:"
    echo "  docker-compose up -d postgres"
    exit 1
fi

# Check if tables exist
if psql -h postgres -U federation -d federation -c "SELECT 1 FROM entity_configurations LIMIT 1;" > /dev/null 2>&1; then
    echo "✅ Database tables already exist"
    
    # Show current statistics
    echo "📊 Current database statistics:"
    psql -h postgres -U federation -d federation -c "SELECT * FROM federation_stats;"
    
    echo ""
    echo "🎉 Database is already set up and ready!"
    exit 0
fi

echo "🔧 Setting up database tables and sample data..."

# Run the initialization script
echo "📝 Creating database tables..."
psql -h postgres -U federation -d federation -f /app/config/init.sql

echo "📊 Populating with sample data..."
psql -h postgres -U federation -d federation -f /app/src/main/resources/sql/sample-data.sql

echo "✅ Database setup completed successfully!"

# Show final statistics
echo ""
echo "📈 Final database statistics:"
psql -h postgres -U federation -d federation -c "SELECT * FROM federation_stats;"

echo ""
echo "🎉 Database is ready for use!"
echo ""
echo "📋 Available sample entities:"
echo "  - https://op.example.com (OpenID Provider)"
echo "  - https://rp.example.com (Relying Party)"
echo "  - https://test-op.example.com (Test OpenID Provider)"
echo ""
echo "🔍 You can test the API endpoints:"
echo "  curl http://localhost:8080/federation/metadata"
echo "  curl http://localhost:8080/database/stats"
echo "  curl http://localhost:8080/database/health"

