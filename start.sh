#!/bin/sh

echo "🚀 Starting Chicken Calculator System..."

# Start nginx for frontend serving
echo "📱 Starting frontend servers..."
nginx -g "daemon on;"

# Wait a moment for nginx to start
sleep 2

# Start Spring Boot backend
echo "⚙️  Starting backend server..."
exec java -Xmx512m \
    -Dspring.profiles.active=production \
    -Dserver.port=8080 \
    -jar chicken-calculator-1.0.0.jar