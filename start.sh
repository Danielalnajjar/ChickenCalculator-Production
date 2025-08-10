#!/bin/sh

echo "🚀 Starting Chicken Calculator System..."

# Create data directory for H2 database
echo "📁 Creating data directory..."
mkdir -p /app/data

# Start Spring Boot backend on port 8081
echo "⚙️  Starting Spring Boot backend on port 8081..."
java -Xmx512m \
    -Dserver.port=8081 \
    -jar chicken-calculator-1.0.0.jar &

# Wait for Spring Boot to start
echo "⏳ Waiting for backend to start..."
sleep 15

# Start nginx on port 8080 as reverse proxy
echo "📱 Starting nginx on port 8080..."
exec nginx -g "daemon off;"