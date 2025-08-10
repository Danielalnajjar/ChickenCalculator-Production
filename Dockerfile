# Production-optimized multi-stage build for Railway deployment
FROM node:18-alpine AS frontend-build

# Install security updates
RUN apk update && apk upgrade && apk add --no-cache dumb-init

# Build React Admin Portal
WORKDIR /app/admin-portal
COPY admin-portal/package*.json ./
RUN npm install --legacy-peer-deps
COPY admin-portal/ ./
RUN npm run build

# Build main React frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install --legacy-peer-deps
COPY frontend/ ./
RUN npm run build

# Build Spring Boot backend
FROM maven:3.9.5-eclipse-temurin-17 AS backend-build

WORKDIR /app
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B

COPY backend/src ./src
RUN mvn clean package -DskipTests -B -q

# Final production image with security hardening
FROM eclipse-temurin:17-jre-alpine

# Install security updates and create non-root user
RUN apk update && apk upgrade && apk add --no-cache \
    dumb-init \
    nginx \
    curl \
    && addgroup -g 1000 appgroup \
    && adduser -u 1000 -G appgroup -s /bin/sh -D appuser \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# Copy built applications with proper ownership
COPY --from=backend-build --chown=appuser:appgroup /app/target/chicken-calculator-1.0.0.jar ./
COPY --from=frontend-build --chown=appuser:appgroup /app/admin-portal/build ./static/admin
COPY --from=frontend-build --chown=appuser:appgroup /app/frontend/build ./static/app

# Create nginx configuration with security headers
RUN mkdir -p /etc/nginx/http.d
COPY --chown=appuser:appgroup nginx.conf /etc/nginx/http.d/default.conf

# Create startup script with proper permissions
COPY --chown=appuser:appgroup start.sh ./
RUN chmod +x start.sh

# Create directories for nginx and logs
RUN mkdir -p /var/log/nginx /var/lib/nginx /run/nginx \
    && chown -R appuser:appgroup /var/log/nginx /var/lib/nginx /run/nginx /etc/nginx

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]
CMD ["./start.sh"]