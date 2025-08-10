#!/bin/sh

echo "🚀 Starting Chicken Calculator System..."

# Create data directory for H2 database
echo "📁 Creating data directory..."
mkdir -p /app/data

# Start Spring Boot backend on port 8081 in background
echo "⚙️  Starting backend server on port 8081..."
java -Xmx512m \
    -Dspring.profiles.active=production \
    -Dserver.port=8081 \
    -jar chicken-calculator-1.0.0.jar &

# Give Spring Boot time to start
sleep 10

# Start nginx on port 8080 as main entry point
echo "📱 Starting nginx on port 8080..."
exec nginx -g "daemon off;"