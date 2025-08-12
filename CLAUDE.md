# CLAUDE.md - ChickenCalculator Production Deployment

This file provides guidance to Claude Code (claude.ai/code) when working with the Chicken Calculator production system.

## Project Overview

A **multi-tenant chicken calculator system** deployed on Railway with location-based access:

1. **Backend**: Spring Boot 3.2.0 with Kotlin - REST API and multi-tenant logic
2. **Admin Portal**: React 18 interface for managing locations at `/admin`
3. **Frontend**: React calculator app accessible via location slugs
4. **Infrastructure**: Docker-based deployment to Railway with GitHub auto-deploy

## Current Production Status

### Live URLs
- **Main App**: https://chickencalculator-production-production-2953.up.railway.app
- **Admin Portal**: https://chickencalculator-production-production-2953.up.railway.app/admin
- **Location-Specific**: https://chickencalculator-production-production-2953.up.railway.app/{slug}
  - Example: `/fashion-show` for Fashion Show location

### Deployment Configuration
- **GitHub Repository**: https://github.com/Danielalnajjar/ChickenCalculator-Production
- **Auto-Deploy**: Pushes to `main` branch trigger Railway deployment
- **Platform**: Railway (Project ID: 767deec0-30ac-4238-a57b-305f5470b318)
- **Port**: 8080 (Railway constraint - single port exposure)

## Architecture

### System Design
```
Railway Platform (PORT 8080)
└── Spring Boot Application
    ├── /api/** → REST API Controllers
    ├── /admin/** → Admin Portal (React)
    ├── /{slug} → Location-specific calculator
    └── / → Default calculator
```

### Key Components

#### Backend Controllers
- `AdminController` - Authentication and location management
- `LocationSlugController` - Handles /{slug} routing for locations
- `AdminPortalController` - Serves admin portal static assets
- `ChickenCalculatorController` - Calculator business logic
- `SalesDataController` - Sales data management
- `MarinationLogController` - Marination tracking
- `HealthController` - Health check endpoints

#### Services
- `AdminService` - Admin user management and initialization
- `LocationService` - Multi-tenant location management with slug generation
- `ChickenCalculatorService` - Core calculator logic

#### Frontend Services
- `admin-portal/src/services/api.ts` - Centralized API service with consistent token management

### Database Schema
- `admin_users` - System administrators
- `locations` - Calculator instances with unique slugs
- `sales_data` - Historical sales (location-scoped)
- `marination_log` - Marination history (location-scoped)

## Multi-Tenant Location System

### How It Works
1. **Location Creation**: Admin creates location via admin portal
2. **Slug Generation**: System auto-generates URL-friendly slug from name
3. **Access**: Users access location-specific calculator at `/{slug}`
4. **Data Isolation**: Each location has separate sales and marination data

### Location Routing
- `LocationSlugController` validates slugs against database
- Valid slugs serve the React app with location context headers
- React app receives location info via X-Location-* headers

### Default Location
- System creates "Main Calculator" at `/main` on startup
- Ensures calculator is always accessible even without locations

## Development Setup

### Backend Development
```bash
cd backend
mvn spring-boot:run

# Build JAR
mvn clean package -DskipTests

# Run with custom port
java -Dserver.port=8081 -jar target/chicken-calculator-1.0.0.jar
```

### Frontend Development
```bash
# Admin Portal (custom webpack config)
cd admin-portal
npm install --legacy-peer-deps
npm run build

# Main App
cd frontend
npm install --legacy-peer-deps
npm run build
```

### Docker Local Testing
```bash
docker build -t chicken-calculator .
docker run -p 8080:8080 -e PORT=8080 chicken-calculator
```

## Railway Deployment

### GitHub Auto-Deploy
```bash
git add .
git commit -m "Your changes"
git push origin main
# Railway automatically deploys from GitHub
```

### Railway MCP Server Setup
```bash
# Set Railway API token (Windows)
set RAILWAY_API_TOKEN=<YOUR_RAILWAY_API_TOKEN>

# Add Railway MCP server to Claude Code
claude mcp add railway -- npx -y @jasontanswe/railway-mcp

# Verify connection
claude mcp list  # Should show "railway: ✓ Connected"
```

**Note**: Keep the API token secure. Never commit it to the repository.

## API Documentation

### Public Endpoints (No Auth Required)
- `GET /api/health` - System health status
- `GET /api/calculator/**` - Calculator operations
- `GET /api/sales-data/**` - Sales data (location-scoped)
- `GET /api/marination-log/**` - Marination logs
- `GET /{slug}` - Location-specific calculator access

### Admin Endpoints (Auth Required)
- `POST /api/admin/auth/login` - Admin login
- `POST /api/admin/auth/validate` - Token validation
- `GET /api/admin/locations` - List all locations
- `POST /api/admin/locations` - Create new location
- `DELETE /api/admin/locations/{id}` - Delete location

## Security & Authentication

### Current Security State
- **BCrypt Password Hashing**: 10 rounds
- **JWT Tokens**: Stored in sessionStorage
- **CORS**: Configured for specific Railway domains
- **Default Admin**: `admin@yourcompany.com` / `Admin123!`

### Token Management
- Centralized in `admin-portal/src/services/api.ts`
- Consistent TOKEN_KEY = 'chicken_admin_token'
- Automatic 401 handling with redirect to login

## Environment Variables

### Required
- `PORT` - Set by Railway (8080)
- `ADMIN_DEFAULT_PASSWORD` - Override default admin password
- `SPRING_PROFILES_ACTIVE` - Use `production` for Railway

### Optional
- `FORCE_ADMIN_RESET` - Force recreate admin on startup
- `DATABASE_URL` - PostgreSQL connection (defaults to H2)

## Common Issues & Solutions

### Location Access Returns 403
- **Cause**: SecurityConfig not allowing slug routes
- **Solution**: Ensure `/{slug}` and `/{slug}/**` are in permitAll()

### Authentication Token Mismatch
- **Cause**: Inconsistent token key between components
- **Solution**: Use centralized API service with TOKEN_KEY constant

### Static Files Not Loading
- **Cause**: WebConfig resource handlers misconfigured
- **Solution**: Check `/app/static/app` and `/app/static/admin` paths

### Build Memory Issues on Railway
- **Solution**: Set NODE_OPTIONS="--max-old-space-size=1024" in Dockerfile

## Recent Updates (December 2024)

### Authentication Fixes
- Created centralized API service for consistent token management
- Fixed token key mismatch between AuthContext and components
- Added proper TypeScript typing for API responses

### Location Slug Routing
- Implemented LocationSlugController for /{slug} routes
- Added security configuration for public location access
- Enabled multi-tenant calculator access via custom URLs

### Build Optimizations
- Moved build dependencies to dependencies (not devDependencies)
- Disabled webpack optimization to prevent memory issues
- Added custom webpack config for admin portal

## Important Files

### Configuration
- `backend/src/main/resources/application.yml` - Spring configuration
- `Dockerfile` - Multi-stage build setup
- `railway.json` - Railway platform config
- `admin-portal/webpack.config.js` - Custom webpack (no react-scripts)

### Security
- `backend/.../SecurityConfig.kt` - Spring Security setup
- `backend/.../JwtAuthenticationFilter.kt` - JWT validation
- `admin-portal/src/services/api.ts` - Frontend auth service

### Routing
- `backend/.../LocationSlugController.kt` - Location slug handler
- `backend/.../WebConfig.kt` - Static resource configuration

## Testing Checklist

1. ✅ Admin can login at `/admin`
2. ✅ Admin can create new locations
3. ✅ Locations accessible via slug URLs
4. ✅ Calculator works for each location
5. ✅ Sales data persists per location
6. ✅ Health check returns UP status
7. ✅ Static assets load correctly
8. ✅ JWT tokens properly validated

## Notes for Future Development

- Railway only exposes single port (8080)
- Always use GitHub push for deployment (not Railway CLI)
- Test authentication changes locally before deploying
- Keep Railway API token secure and never commit it
- Location slugs must be unique and URL-safe
- Default location ensures system always has one working calculator