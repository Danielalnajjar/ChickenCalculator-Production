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
    
    # Check for static subdirectory
    if [ -d "/app/static/admin/static" ]; then
        echo "   📁 Admin static subdirectory found:"
        ls -la /app/static/admin/static/ | head -5
        
        # Check for JS files
        if [ -d "/app/static/admin/static/js" ]; then
            echo "   📜 JavaScript files:"
            ls -la /app/static/admin/static/js/ | head -3
        fi
        
        # Check for CSS files
        if [ -d "/app/static/admin/static/css" ]; then
            echo "   🎨 CSS files:"
            ls -la /app/static/admin/static/css/ | head -3
        fi
    else
        echo "   ⚠️ No static subdirectory at /app/static/admin/static"
    fi
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