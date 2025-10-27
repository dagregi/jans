#!/bin/bash

# Database initialization script for Jans Federation Vibe
# This script ensures the database is properly initialized with tables and sample data

set -e

echo "🗄️ Initializing Jans Federation Vibe database..."

# Wait for database to be ready
echo "⏳ Waiting for database to be ready..."
until pg_isready -h postgres -p 5432 -U federation; do
    echo "Database is unavailable - sleeping"
    sleep 2
done

echo "✅ Database is ready!"

# Check if database is already initialized
echo "🔍 Checking if database is already initialized..."
if psql -h postgres -U federation -d federation -c "SELECT 1 FROM entity_configurations LIMIT 1;" > /dev/null 2>&1; then
    echo "✅ Database is already initialized with sample data"
    exit 0
fi

echo "📊 Database is not initialized, running initialization..."

# Run database initialization
echo "🔧 Creating database tables and sample data..."

# Execute the initialization script
psql -h postgres -U federation -d federation -f /app/config/init.sql

echo "✅ Database initialization completed successfully!"
echo ""
echo "📈 Database statistics:"
psql -h postgres -U federation -d federation -c "SELECT * FROM federation_stats;"

echo ""
echo "🎉 Database is ready for use!"
