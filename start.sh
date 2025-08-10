#!/bin/sh

echo "🚀 Starting Chicken Calculator System..."

# Create data directory for H2 database
echo "📁 Creating data directory..."
mkdir -p /app/data

# Spring Boot will use Railway's PORT env var (8080)
# It serves both API and static files
echo "⚙️  Starting Spring Boot server..."
echo "📱 Server will handle:"
echo "   - API endpoints at /api/*"
echo "   - Admin portal at /admin/*"
echo "   - Main app at /*"

exec java -Xmx512m \
    -jar chicken-calculator-1.0.0.jar