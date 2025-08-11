#!/bin/sh

echo "🚀 Starting Chicken Calculator System..."

# Create data directory for H2 database
echo "📁 Creating data directory..."
mkdir -p /app/data

# List static files for debugging
echo "📂 Checking static files..."
if [ -d "/app/static/admin" ]; then
    echo "   ✅ Admin portal files found:"
    ls -la /app/static/admin/ | head -5
else
    echo "   ❌ Admin portal files NOT FOUND at /app/static/admin"
fi

if [ -d "/app/static/app" ]; then
    echo "   ✅ Main app files found:"
    ls -la /app/static/app/ | head -5
else
    echo "   ❌ Main app files NOT FOUND at /app/static/app"
fi

# Spring Boot will use Railway's PORT env var (8080)
# It serves both API and static files
echo "⚙️  Starting Spring Boot server..."
echo "📱 Server will handle:"
echo "   - API endpoints at /api/*"
echo "   - Admin portal at /admin/*"
echo "   - Main app at /*"

exec java -Xmx512m \
    -jar chicken-calculator-1.0.0.jar