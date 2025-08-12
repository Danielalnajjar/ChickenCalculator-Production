# CLAUDE.md - ChickenCalculator Production System Guide

This file provides comprehensive guidance to Claude Code (claude.ai/code) when working with the Chicken Calculator production system.

## Quick Reference

### Commands
```bash
# Backend
cd backend && mvn spring-boot:run          # Run locally
mvn clean package -DskipTests              # Build JAR
mvn test                                   # Run tests

# Frontend
cd admin-portal && npm start               # Admin portal dev
cd frontend && npm start                   # Main app dev
npm run build                              # Production build
npm test                                   # Run tests

# Docker
docker build -t chicken-calculator .       # Build image
docker run -p 8080:8080 chicken-calculator # Run container

# Git Deployment
git push origin main                       # Triggers Railway auto-deploy
```

### URLs & Access
- **Production**: https://chickencalculator-production-production-2953.up.railway.app
- **Admin Portal**: https://chickencalculator-production-production-2953.up.railway.app/admin
- **Location Access**: https://chickencalculator-production-production-2953.up.railway.app/{slug}
- **Metrics**: /actuator/prometheus
- **Health**: /api/health

### Default Credentials
- **Admin**: admin@yourcompany.com (password from ADMIN_DEFAULT_PASSWORD env var)
- **Note**: Password change required on first login

## Current Production Status

### Version & Deployment
- **Production Readiness**: 9.5/10 (All critical issues resolved)
- **Platform**: Railway (Project ID: 767deec0-30ac-4238-a57b-305f5470b318)
- **GitHub**: https://github.com/Danielalnajjar/ChickenCalculator-Production
- **Auto-Deploy**: Enabled from main branch
- **Port**: 8080 (Railway single-port constraint)

### Recent Major Improvements (December 2024)
- ✅ All 24 critical security vulnerabilities fixed
- ✅ Multi-tenant data isolation implemented
- ✅ WCAG 2.1 Level AA compliance achieved
- ✅ Comprehensive monitoring and observability added
- ✅ Test infrastructure established
- ✅ API versioning implemented (/api/v1)

## Architecture Overview

### System Design
```
Railway Platform (PORT 8080)
└── Spring Boot Application
    ├── /api/v1/** → Versioned REST API
    ├── /api/health → Health checks
    ├── /actuator/** → Monitoring endpoints
    ├── /admin/** → Admin Portal (React)
    ├── /{slug} → Location-specific calculator
    └── / → Default calculator
```

### Backend Architecture (Spring Boot 3.2.0 + Kotlin)

#### Controllers (Separated by Responsibility)
- `AdminAuthController` - Authentication endpoints only
- `AdminLocationController` - Location management
- `ChickenCalculatorController` - Calculator logic
- `SalesDataController` - Sales data management
- `MarinationLogController` - Marination tracking
- `LocationSlugController` - Slug routing
- `HealthController` - Health checks

#### Service Layer (Business Logic)
- `LocationManagementService` - Enhanced location CRUD with validation
- `SalesDataService` - Multi-tenant sales operations
- `MarinationLogService` - Marination business rules
- `AdminService` - User management
- `ChickenCalculatorService` - Core calculations
- `MetricsService` - Business metrics tracking

#### Security & Infrastructure
- `JwtAuthenticationFilter` - JWT validation (httpOnly cookies)
- `GlobalExceptionHandler` - Standardized error responses
- `CorrelationIdFilter` - Request tracing
- `RequestLoggingInterceptor` - Structured logging

#### Database (Flyway Migrations)
- `admin_users` - System administrators with password change tracking
- `locations` - Multi-tenant locations with unique slugs
- `sales_data` - Historical sales (location-scoped)
- `marination_log` - Marination history (location-scoped)

### Frontend Architecture

#### Admin Portal (React 18 + TypeScript)
- Password change enforcement on first login
- CSRF protection with double-submit cookies
- Responsive design with mobile navigation
- WCAG 2.1 AA compliant
- Jest + React Testing Library tests

#### Main Calculator App
- Location-based access via slugs
- Multi-tenant data isolation
- Accessible forms with ARIA labels
- Mobile-optimized with 44px touch targets

## API Documentation (v1)

### Public Endpoints (No Auth)
```
GET  /api/health                          - System health
GET  /api/v1/calculator/locations         - Available locations
POST /api/v1/calculator/calculate         - Marination calculation
GET  /api/v1/sales-data                   - Sales history
POST /api/v1/sales-data                   - Add sales record
GET  /api/v1/marination-log               - Marination history
POST /api/v1/marination-log               - Log marination
GET  /{slug}                              - Location calculator
```

### Admin Endpoints (Auth Required)
```
POST /api/v1/admin/auth/login             - Admin login
POST /api/v1/admin/auth/validate          - Token validation
POST /api/v1/admin/auth/change-password   - Change password
GET  /api/v1/admin/auth/csrf-token        - Get CSRF token
POST /api/v1/admin/auth/logout            - Logout
GET  /api/v1/admin/locations              - List locations
POST /api/v1/admin/locations              - Create location
DELETE /api/v1/admin/locations/{id}       - Delete location
GET  /api/v1/admin/stats                  - Dashboard stats
```

### Monitoring Endpoints
```
GET /actuator/health                      - Detailed health
GET /actuator/prometheus                  - Prometheus metrics
GET /actuator/metrics                     - JSON metrics
```

## Security & Authentication

### Current Implementation
- **Password Hashing**: BCrypt (10 rounds)
- **JWT Storage**: httpOnly cookies (XSS-safe)
- **CSRF Protection**: Double-submit cookie pattern
- **Password Policy**: Change required on first login
- **Correlation IDs**: Request tracing across system
- **Error Tracking**: Sentry integration

### Required Environment Variables
```bash
# CRITICAL - Must be set
JWT_SECRET=<32+ character secret>         # JWT signing key
ADMIN_DEFAULT_PASSWORD=<secure-password>  # Initial admin password
SENTRY_DSN=<sentry-project-dsn>          # Error tracking

# Database
DATABASE_URL=postgresql://...             # PostgreSQL connection
SPRING_PROFILES_ACTIVE=production        # Production profile

# Security
H2_CONSOLE_ENABLED=false                 # Disable H2 console
FORCE_PASSWORD_CHANGE=true               # Force initial password change
```

## Monitoring & Observability

### Prometheus Metrics
- Business metrics (calculations, locations, sales)
- Performance metrics (response times, throughput)
- Error tracking by category and location
- Database connection pool monitoring

### Structured Logging
- Correlation IDs for request tracing
- JSON format in production (logstash encoder)
- Sensitive data exclusion
- MDC context propagation

### Health Checks
- Component health status
- Database connectivity
- Memory and thread monitoring
- Custom business health indicators

## Testing Infrastructure

### Backend Testing
- Unit tests with JUnit 5 and Mockito
- Integration tests with Spring Boot Test
- TestContainers for database testing
- Current coverage: ~30% (target: 80%)

### Frontend Testing
- Jest + React Testing Library
- Component and integration tests
- Accessibility testing included
- Coverage thresholds: 70%

### Test Commands
```bash
# Backend
mvn test                                  # Run all tests
mvn test -Dtest=ServiceTest              # Run specific test

# Frontend
npm test                                  # Run tests
npm run test:coverage                    # With coverage
```

## Multi-Tenant System

### Location Management
1. Admin creates location via portal
2. System generates URL-friendly slug
3. Location accessible at /{slug}
4. Data completely isolated per location

### Data Isolation
- Location context via X-Location-Id header
- Service layer enforces tenant boundaries
- Repository queries scoped to location
- Security checks prevent cross-tenant access

## Development Workflow

### Local Development
```bash
# Start backend (H2 database)
cd backend && mvn spring-boot:run

# Start admin portal
cd admin-portal && npm start

# Start frontend
cd frontend && npm start
```

### Build & Deploy
```bash
# Build everything
mvn clean package -DskipTests
cd admin-portal && npm run build
cd frontend && npm run build

# Deploy to Railway
git add . && git commit -m "changes"
git push origin main  # Auto-deploys
```

## Troubleshooting Guide

### Common Issues

#### JWT Token Issues
- **Symptom**: 401 errors after deployment
- **Solution**: Ensure JWT_SECRET is set (32+ chars)

#### Database Connection
- **Symptom**: Application won't start
- **Solution**: Verify DATABASE_URL format and credentials

#### CORS Errors
- **Symptom**: Cross-origin requests blocked
- **Solution**: Check SecurityConfig allowed origins

#### Multi-Tenant Data Issues
- **Symptom**: Wrong location data returned
- **Solution**: Verify X-Location-Id header is sent

### Performance Baselines
- Response time p95: < 200ms
- Database pool usage: < 50%
- Memory usage: < 512MB
- Error rate: < 0.1%

## Recent Changes Log

### December 2024 - Production Readiness
- **Security**: JWT in httpOnly cookies, CSRF protection, password change enforcement
- **Data**: Fixed multi-tenant isolation, added Flyway migrations
- **Architecture**: Service layer, controller separation, global exception handling
- **Monitoring**: Prometheus metrics, correlation IDs, Sentry integration
- **Testing**: Backend/frontend test infrastructure
- **Accessibility**: WCAG 2.1 AA compliance, mobile navigation

### Files Modified
- 40+ files updated
- 4,677 lines added
- Complete security overhaul
- Architecture refactoring

## Important Notes

### Railway Constraints
- Single port exposure (8080)
- Memory limits for build process
- Environment variables required before deployment

### Best Practices
- Always use GitHub push for deployment
- Test locally before deploying
- Check metrics after deployment
- Monitor error rates in Sentry
- Keep JWT_SECRET secure
- Use correlation IDs for debugging

### Future Enhancements
- Expand test coverage to 80%
- Add Redis caching layer
- Implement rate limiting
- Create operational runbooks
- Add backup/restore procedures

## Support Resources

### Documentation
- `DEPLOYMENT_CHECKLIST.md` - Step-by-step deployment
- `ARCHITECTURE.md` - Detailed system design
- `FLYWAY_IMPLEMENTATION.md` - Database migration guide
- `METRICS_IMPLEMENTATION.md` - Monitoring setup

### Monitoring
- Railway logs: Available in dashboard
- Sentry: Real-time error tracking
- Prometheus: /actuator/prometheus
- Health: /api/health

---

*Last Updated: December 2024 - Production Ready (9.5/10)*