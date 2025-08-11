# CLAUDE.md - ChickenCalculator Production Deployment

This file provides guidance to Claude Code (claude.ai/code) when working with the production deployment system for the Chicken Calculator.

## Project Context

This is a **production deployment system** for the Chicken Calculator application, designed to deploy multiple calculator instances to Railway cloud platform. It consists of:

1. **Backend**: Spring Boot 3.2.0 with Kotlin - Admin API and calculator services
2. **Admin Portal**: React 18 admin interface for managing deployments
3. **Frontend**: React calculator app for end users
4. **Infrastructure**: Docker-based deployment to Railway

## Current Status & Critical Context

### ⚠️ ACTIVE DEBUGGING STATE
The system currently has authentication bypass enabled for debugging Railway deployment issues:
- **ANY non-empty login credentials will work** (see AdminController.kt lines 61-70)
- Request logging is active to debug routing issues
- Test endpoint available at `/test.html` for API connectivity testing

### Known Railway Constraints
- **Single PORT exposure**: Railway only exposes port 8080
- Cannot run nginx and Spring Boot on separate ports
- Solution: Spring Boot serves all static files via WebConfig

### Authentication System Status
- Default admin: `admin@yourcompany.com` / `Admin123!`
- Database: H2 file-based at `/app/data/chicken-calculator-db`
- Password hashing: BCrypt (temporarily bypassed for debugging)
- Admin initialization runs on startup via AdminService

## Development Commands

### Backend (Spring Boot)
```bash
# Local development
cd backend
mvn spring-boot:run

# Build JAR
mvn clean package -DskipTests

# Run with specific port
java -Dserver.port=8081 -jar target/chicken-calculator-1.0.0.jar
```

### Admin Portal (React)
```bash
cd admin-portal
npm install --legacy-peer-deps
npm run build  # Uses custom webpack.config.js

# Note: NO react-scripts - custom webpack due to dependency conflicts
```

### Frontend (Main App)
```bash
cd frontend
npm install --legacy-peer-deps
npm run build
```

### Docker Build
```bash
# Full production build
docker build -t chicken-calculator .

# Run locally
docker run -p 8080:8080 -e PORT=8080 chicken-calculator
```

### Railway Deployment
```bash
# Push to GitHub (Railway auto-deploys from main branch)
git add .
git commit -m "Deploy to Railway"
git push origin main
```

## Architecture Overview

### Deployment Architecture (Single-Service Model)
```
Railway Platform (PORT 8080)
    └── Spring Boot Application
        ├── /api/** → REST Controllers
        ├── /admin/** → Admin Portal (React)
        ├── /** → Main App (React)
        └── /test.html → Debug Interface
```

### Key Components

#### Backend Structure
- **Controllers**: 
  - `AdminController` - Authentication, location management
  - `ChickenCalculatorController` - Calculator API
  - `SalesDataController` - Sales data management
  - `MarinationLogController` - Marination history

- **Services**:
  - `AdminService` - Admin user management, initialization
  - `LocationService` - Multi-tenant location management
  - `DeploymentService` - Railway deployment automation
  - `ChickenCalculatorService` - Business logic wrapper

- **Configuration**:
  - `WebConfig` - Static file serving, React Router support
  - `RequestLoggingConfig` - Debug request logging

#### Database Schema
- `admin_users` - System administrators
- `locations` - Deployed calculator instances
- `sales_data` - Historical sales records
- `marination_log` - Marination history

## Critical Files & Locations

### Configuration Files
- `backend/src/main/resources/application.yml` - Main Spring config
- `backend/src/main/resources/application-production.yml` - Production overrides
- `Dockerfile` - Multi-stage build configuration
- `start.sh` - Railway startup script
- `railway.json` - Railway platform configuration

### Debugging Tools
- `backend/src/main/resources/static/test.html` - API test interface
- `backend/.../RequestLoggingConfig.kt` - Request logging
- `backend/.../AdminController.kt` - Debug endpoints (/api/admin/test, /api/admin/debug/status)

### Build Configurations
- `admin-portal/webpack.config.js` - Custom webpack (replaced react-scripts)
- `admin-portal/tsconfig.json` - TypeScript config (noEmit: false)
- `backend/pom.xml` - Maven configuration

## Problem History & Solutions

### 1. React Build Issues
**Problem**: ajv/ajv-keywords dependency conflicts with react-scripts 5.0.1
**Solution**: Removed react-scripts, implemented custom webpack configuration

### 2. Railway Port Conflicts
**Problem**: nginx and Spring Boot both trying to use port 8080
**Solution**: Single-service architecture - Spring Boot serves everything

### 3. Authentication Failures
**Problem**: Persistent "bad credentials" errors on Railway
**Current State**: Debugging with bypass and extensive logging
**Suspected Causes**:
- CORS issues between frontend and backend
- Request routing problems
- Database persistence issues

### 4. Database Persistence
**Problem**: H2 in-memory database losing data on restart
**Solution**: Switched to file-based H2 with proper directory permissions

## Testing & Debugging

### Test Authentication
1. Access `/test.html` on deployed app
2. Click test buttons to verify API connectivity
3. Check Railway logs for request logging output

### Verify Admin Initialization
```bash
# Check Railway logs for:
"🔧 ADMIN INITIALIZATION"
"✅ Default admin created"
```

### Monitor Requests
All requests log with format:
```
🔍 REQUEST: [METHOD] [URI]
   Headers: [...]
   From: [IP]
```

### API Endpoints for Testing
- `GET /api/admin/test` - Simple connectivity test
- `GET /api/admin/debug/status` - Admin system status
- `POST /api/admin/auth/login` - Authentication (currently bypassed)

## Environment Variables

### Required for Production
- `PORT` - Set by Railway (usually 8080)
- `ADMIN_DEFAULT_PASSWORD` - Override default admin password
- `FORCE_ADMIN_RESET` - Force recreate admin user on startup

### Optional
- `SPRING_PROFILES_ACTIVE` - Spring profile (production, docker)
- `SERVER_PORT` - Override port (ignored if PORT is set)

## Deployment Checklist

1. ✅ Verify all builds pass locally
2. ✅ Test authentication flow with bypass
3. ⚠️ Remove authentication bypass before production
4. ⚠️ Re-enable proper BCrypt password verification
5. ✅ Ensure database persistence configured
6. ✅ Verify CORS configuration
7. ✅ Test all API endpoints
8. ⚠️ Remove debug logging for production

## Common Issues & Solutions

### "Bad Credentials" Error
1. Check if request reaches backend (RequestLoggingConfig)
2. Verify CORS headers in response
3. Check admin exists in database
4. Test with `/test.html` interface

### Build Failures on Railway
1. Check Maven/npm versions match Railway environment
2. Verify all dependencies resolved
3. Check Dockerfile COPY commands
4. Review Railway build logs

### Static Files Not Serving
1. Verify WebConfig resource handlers
2. Check file paths in Docker container
3. Ensure proper file permissions (appuser:appgroup)

## Next Steps

1. **Fix Authentication**: Identify root cause of login failures
2. **Remove Debug Code**: Clean up bypasses and excessive logging
3. **Implement Proper Multi-tenancy**: Complete location isolation
4. **Add Monitoring**: Implement health checks and metrics
5. **Security Hardening**: Re-enable all security features

## Important Notes

- **DO NOT** commit sensitive credentials to repository
- **ALWAYS** test authentication changes locally first
- **REMEMBER** Railway only exposes one port (8080)
- **CHECK** Railway logs after each deployment
- **CURRENT STATE** is debugging mode - not production ready