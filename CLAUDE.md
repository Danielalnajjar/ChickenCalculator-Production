# CLAUDE.md - ChickenCalculator Production System Guide

This file provides comprehensive guidance to Claude Code (claude.ai/code) when working with the Chicken Calculator production system.

**Last Updated**: January 14, 2025
**System Version**: 1.0.0 (Spring Boot 3.2.0 + Kotlin 1.9.20 + React 18.2.0)
**Production Status**: ✅ Fully Operational (10/10)
**Error Monitoring**: ✅ Sentry 7.14.0 Active (Re-enabled January 14, 2025)
**Test Coverage**: ⚠️ ~30% Backend (config broken), ~20% Frontend

## Quick Reference

### Commands
```bash
# Backend
cd backend && mvn spring-boot:run          # Run locally
cd backend && mvn spring-boot:run -Dspring.profiles.active=dev  # Run with debug tools
mvn clean package -DskipTests              # Build JAR
mvn test                                   # Run tests (⚠️ currently broken - see Known Issues)
mvn compile                                # Quick compilation check

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

# Development Scripts (Windows)
.\run-dev.bat                              # Start with dev profile
.\run-dev-test.bat                         # Start with test settings
.\test-profile-isolation.bat              # Test profile isolation
.\test-local-prod.bat                      # Test production profile locally
```

### URLs & Access
- **Production**: https://chickencalculator-production-production-2953.up.railway.app
- **Admin Portal**: https://chickencalculator-production-production-2953.up.railway.app/admin
- **Location Access**: https://chickencalculator-production-production-2953.up.railway.app/{slug}
  - Location Login: /{slug}/ (password protected)
  - Calculator: /{slug}/calculator
  - Sales Data: /{slug}/sales
  - Marination History: /{slug}/history
- **Metrics**: /actuator/prometheus
- **Health**: /api/health

### Default Credentials
- **Admin**: admin@yourcompany.com (password from ADMIN_DEFAULT_PASSWORD env var)
  - **Note**: Password change required on first login
- **Locations**: Each location has its own password
  - **Default**: "ChangeMe123!" (set via V5 migration)
  - **Note**: Admins can generate/update location passwords

### Debug Endpoints (Dev Profile Only)
- **/probe/ok** - Basic health probe
- **/probe/boom** - Exception testing
- **/minimal** - Minimal functionality test
- **/debug/mappings** - Spring mapping inspection
- **/debug/converters** - HTTP converter debugging
- **/test** - Simple test endpoint
- **/test-html** - HTML test endpoint

## Current Production Status

### ✅ RESOLVED: Servlet 500 Errors Fixed & Security Hardened (January 13, 2025)
- **Issue**: All custom endpoints were returning 500 errors in production
- **Root Cause**: Spring 6's PathPatternParser doesn't allow /** patterns
- **Solution**: Comprehensive security hardening and pattern fixes
- **Key Improvements**:
  - Removed all /** patterns from SpaController and SecurityConfig
  - Implemented ResponseCookie with SameSite=Strict for JWT security
  - Added security headers (CSP, X-Content-Type-Options, X-Frame-Options)
  - Restricted actuator to health,info endpoints only
  - Added @Profile("dev") protection for debug endpoints
  - Created Cookies.kt utility for centralized JWT cookie management
- **Status**: ✅ All endpoints working correctly with enhanced security

### ✅ Sentry Error Monitoring Re-enabled (January 14, 2025)
- **Previous Issue**: Sentry was disabled thinking it caused servlet errors (it didn't)
- **Root Cause Analysis**: The real issue was Spring 6 /** patterns, not Sentry
- **Solution**: Re-enabled Sentry 7.14.0 with production-safe configuration
- **Configuration**:
  - Comprehensive noise filtering (health/actuator/static excluded)
  - Conservative sampling (10% traces, 1% profiles)
  - Multi-tenant location context tracking (non-PII)
  - No PII collection (send-default-pii: false)
  - Resilient error handling with 5-second timeouts
- **DSN**: Connected to wok-to-walk/java-spring-boot project
- **Status**: ✅ Full observability restored with active error monitoring

### Version & Deployment
- **Production Readiness**: 10/10 ✅ (Fully operational)
- **Database**: PostgreSQL 16.8 on Railway (V5 migration applied Jan 12, 2025)
- **Platform**: Railway - **CRITICAL IDs for MCP Commands**:
  - **Project ID**: `767deec0-30ac-4238-a57b-305f5470b318`
  - **Service ID**: `fde8974b-10a3-4b70-b5f1-73c4c5cebbbe`
  - **Postgres ID**: `bbbadbce-026c-44f1-974c-00d5a457bccf`
  - **Environment ID**: `f57580c2-24dc-4c4e-adf2-313399c855a9`
- **GitHub**: https://github.com/Danielalnajjar/ChickenCalculator-Production
- **Auto-Deploy**: Enabled from main branch
- **Port**: 8080 (Railway single-port constraint)

### Latest Status (January 14, 2025)
- **Backend**: ✅ Fully compilable and operational
- **Database**: ✅ PostgreSQL with V5 migration (location auth)
- **Tests**: ✅ All compile successfully with new regression tests
- **Production**: ✅ All endpoints working correctly
- **Multi-Location**: ✅ Auth system complete
- **Controllers**: ✅ Using @RestController with Spring 6 compatible patterns
- **Security**: ✅ ResponseCookie with SameSite support implemented
- **Hardening**: ✅ Security headers (CSP, X-Content-Type-Options) added
- **Monitoring**: ✅ Sentry 7.14.0 active with comprehensive filtering
- **Observability**: ✅ Error tracking, logging, and performance monitoring enabled

### Recent Changes (December 2024 - January 2025)
- ✅ All 25 critical security vulnerabilities fixed
- ✅ Multi-tenant data isolation with location authentication
- ✅ WCAG 2.1 Level AA compliance achieved
- ✅ Comprehensive monitoring and observability added
- ✅ Test infrastructure established
- ✅ API versioning implemented (/api/v1)
- ✅ All compilation errors resolved (26 fixes applied)
- ✅ PostgreSQL migration completed successfully
- ✅ Password change feature FIXED (December 12, 2024)
- ✅ Multi-location authentication system (January 12, 2025)
- ✅ Servlet 500 errors RESOLVED (January 13, 2025)
- ✅ Security hardening: ResponseCookie, CSP headers, actuator restrictions (January 13, 2025)
- ✅ Spring 6 compatibility: All /** patterns removed, custom RequestMatchers implemented (January 13, 2025)
- ✅ Test coverage expansion: SpaControllerTest, SecurityConfigTest added (January 13, 2025)
- ✅ Sentry error monitoring RE-ENABLED with production-safe config (January 14, 2025)

## Dependencies & Technology Stack

### Backend Dependencies
- **Spring Boot**: 3.2.0 (Spring 6, Jakarta EE)
- **Kotlin**: 1.9.20 with Spring/JPA plugins
- **Java**: 17+ required
- **JWT**: jjwt 0.11.5 (consider upgrading to 0.12.x)
- **Database**: PostgreSQL 16.8 + Flyway 10.4.0
- **Monitoring**: Micrometer + Prometheus + Sentry 7.14.0 (Active)
- **Testing**: JUnit 5 + Mockito-Kotlin 5.1.0 + TestContainers 1.19.0
- **Documentation**: SpringDoc OpenAPI 2.2.0

### Frontend Dependencies
- **React**: 18.2.0 + React DOM 18.2.0
- **TypeScript**: 4.9.5 (consider upgrading to 5.x)
- **Routing**: React Router 6.18.0
- **HTTP**: Axios 1.6.0
- **UI**: TailwindCSS 3.3.5 + Heroicons 2.0.18
- **Build**: Webpack 5.89.0 (custom config)
- **Testing**: Jest 29.7.0 + React Testing Library 13.4.0

## Architecture Overview

### System Design
```
Railway Platform (PORT 8080)
└── Spring Boot Application
    ├── /api/v1/** → Versioned REST API
    ├── /api/health → Health checks
    ├── /actuator/** → Monitoring endpoints
    ├── /admin/** → Admin Portal (React)
    ├── /{slug} → Location-specific calculator (protected)
    ├── /{slug}/calculator → Calculator view
    ├── /{slug}/sales → Sales data management
    ├── /{slug}/history → Marination history
    └── / → Landing page with location list
```

### Backend Architecture (Spring Boot 3.2.0 + Kotlin)

#### Controllers (Separated by Responsibility)

**Production Controllers:**
- `AdminAuthController` - Admin authentication endpoints
- `AdminLocationController` - Location management + password control
- `LocationAuthController` - Location-specific authentication
- `ChickenCalculatorController` - Marination calculation (NOT chicken requirements)
- `SalesDataController` - Sales data management (requires location context)
- `MarinationLogController` - Marination tracking (requires location context)
- `LocationSlugController` - Slug routing and location resolution (@Controller for file serving)
- `HealthController` - Health checks
- `AdminPortalController` - Admin portal static resource serving (@RestController)
- `RootController` - Handles root path "/" and landing page (@RestController)
- `SpaController` - Single Page Application routing (Spring 6 compatible patterns)

**Debug Controllers (Dev Profile Only):**
- `TestController` - Testing endpoints (@Profile("dev"))
- `MinimalController` - Basic functionality testing (@Profile("dev"))
- `DebugController` - Spring mapping inspection (@Profile("dev"))
- `DebugMvcController` - HTTP converter debugging (@Profile("dev"))
- `ProbeController` - Health probing (@Profile("dev"))

#### Service Layer (Business Logic)
- `LocationManagementService` - Enhanced location CRUD with validation
- `LocationAuthService` - Location authentication, password management, rate limiting
- `SalesDataService` - Multi-tenant sales operations (no default fallback)
- `MarinationLogService` - Marination business rules (no default fallback)
- `AdminService` - User management
- `ChickenCalculatorService` - Marination calculations (uses calculateMarination method)
- `MetricsService` - Business metrics tracking (Micrometer 1.12.x compatible)
- `JwtService` - JWT token generation and validation

#### Security & Infrastructure

**Production Components:**
- `JwtAuthenticationFilter` - Admin JWT validation (single chain.doFilter())
- `LocationAuthFilter` - Location-specific JWT validation (single chain.doFilter())
- `GlobalExceptionHandler` - Standardized error responses (18 exception types)
- `CorrelationIdFilter` - Request tracing (HIGHEST_PRECEDENCE)
- `RequestLoggingInterceptor` - Structured logging
- `Cookies.kt` - JWT cookie helper with ResponseCookie and SameSite support

#### Diagnostic Infrastructure (Dev Profile Only)

**Diagnostic Filters (@Profile("dev")):**
- `ErrorTapFilter` - Captures ERROR dispatch at HIGHEST_PRECEDENCE
- `ResponseProbeFilter` - Monitors response lifecycle and mutations
- `AfterCommitGuardFilter` - Detects write-after-commit violations (LOWEST_PRECEDENCE - 1)
- `TailLogFilter` - Comprehensive request/response logging (LOWEST_PRECEDENCE)

**Error Components (@Profile("dev")):**
- `TappingErrorAttributes` - Captures Spring error attributes (@Primary)
- `PlainErrorController` - Plain text error responses

**Diagnostic Utilities (@Profile("dev")):**
- `FilterInventory` - Documents filter registration order at startup
- `MvcDiagnostics` - Logs HTTP message converters at startup
- `MappingsLogger` - Logs all registered request mappings at startup

**Production Utility:**
- `PathUtil` - Normalizes request paths (used by filters)

#### Database (PostgreSQL with Flyway Migrations)
- **Current Version**: PostgreSQL 16.8 on Railway
- **Migration Status**: V5 (All migrations applied)
- **Tables**:
  - `admin_users` - System administrators with password change tracking
  - `locations` - Multi-tenant locations with unique slugs
  - `sales_data` - Historical sales (location-scoped)
  - `marination_log` - Marination history (location-scoped)
  - `flyway_schema_history` - Migration tracking
- **Migrations Applied**:
  - V1: Initial schema with all tables
  - V2: Indexes and constraints
  - V3: PostgreSQL sequences for ID generation
  - V4: Admin password reset (applied Dec 12, 2024)
  - V5: Location authentication fields (applied Jan 12, 2025)

### Frontend Architecture

#### Admin Portal (React 18 + TypeScript)
- Password change enforcement on first login
- CSRF protection with double-submit cookies
- Responsive design with mobile navigation
- WCAG 2.1 AA compliant
- Jest + React Testing Library tests

#### Main Calculator App (React with Location Auth)
- Password-protected location access
- Location-specific authentication context
- Multi-tenant data isolation
- Session management with httpOnly cookies
- Accessible forms with ARIA labels
- Mobile-optimized with 44px touch targets

## API Documentation (v1)

### ⚠️ Important API Changes
- **Marination Calculation**: Uses `MarinationRequest` (NOT `ChickenCalculationRequest`)
- **Service Method**: `calculateMarination()` (NOT `calculateChickenRequirements()`)
- **Response Type**: Returns `CalculationResult` (NOT `ChickenCalculationResponse`)

### MarinationRequest Structure
```kotlin
data class MarinationRequest(
    val inventory: InventoryData,       // Nested object with pansSoy, pansTeriyaki, pansTurmeric
    val projectedSales: ProjectedSales, // Nested object with day0, day1, day2, day3
    val availableRawChickenKg: BigDecimal?,
    val alreadyMarinatedSoy: BigDecimal,
    val alreadyMarinatedTeriyaki: BigDecimal,
    val alreadyMarinatedTurmeric: BigDecimal
)
```

### Public Endpoints (No Auth)
```
GET  /api/health                          - System health
GET  /api/v1/calculator/locations         - Available locations
GET  /                                    - Landing page with locations
```

### Location Endpoints (Location Auth Required)
```
POST /api/v1/location/{slug}/auth/login   - Location login
POST /api/v1/location/{slug}/auth/logout  - Location logout
GET  /api/v1/location/{slug}/auth/validate - Validate session
POST /api/v1/calculator/calculate         - Marination calculation
GET  /api/v1/sales-data                   - Sales history
POST /api/v1/sales-data                   - Add sales record
GET  /api/v1/marination-log               - Marination history
POST /api/v1/marination-log               - Log marination
```

### Admin Endpoints (Admin Auth Required)
```
POST /api/v1/admin/auth/login             - Admin login
POST /api/v1/admin/auth/validate          - Token validation
POST /api/v1/admin/auth/change-password   - Change password
GET  /api/v1/admin/auth/csrf-token        - Get CSRF token
POST /api/v1/admin/auth/logout            - Logout
GET  /api/v1/admin/locations              - List locations
POST /api/v1/admin/locations              - Create location
DELETE /api/v1/admin/locations/{id}       - Delete location
PUT  /api/v1/admin/locations/{id}/password - Update location password
POST /api/v1/admin/locations/{id}/generate-password - Generate password
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
- **JWT Storage**: httpOnly cookies with SameSite support (XSS-safe)
- **Cookie Helper**: `Cookies.kt` utility for ResponseCookie management
- **CSRF Protection**: Double-submit cookie pattern
- **Password Policy**: Change required on first login
- **Security Headers**: CSP, X-Content-Type-Options: nosniff, X-Frame-Options
- **Correlation IDs**: Request tracing across system
- **Error Tracking**: Sentry integration (disabled in prod due to servlet issues)

### Required Environment Variables
```bash
# CRITICAL - Must be set
JWT_SECRET=<32+ character secret>         # JWT signing key (min 32 chars)
ADMIN_DEFAULT_PASSWORD=<secure-password>  # Initial admin password
SENTRY_DSN=<configured>                  # Active - Connected to wok-to-walk/java-spring-boot

# Database (Railway PostgreSQL)
DATABASE_URL=postgresql://...             # Railway provides this automatically
PGUSER=postgres                           # Railway provides this
PGPASSWORD=<auto-generated>              # Railway provides this
PGHOST=<hostname>                         # Railway provides this
PGPORT=<port>                             # Railway provides this
PGDATABASE=railway                       # Railway provides this
SPRING_PROFILES_ACTIVE=production        # Production profile

# Hibernate Settings
DDL_AUTO=validate                        # Use 'validate' in production
DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect

# Security Configuration (Optional)
JWT_COOKIE_SAMESITE=Strict              # Cookie SameSite policy (Strict/Lax/None)

# Database Connection Pool (Optional)
DB_POOL_SIZE=15                         # Max connection pool size
DB_MIN_IDLE=5                            # Minimum idle connections
DB_CONNECTION_TIMEOUT=30000             # Connection timeout in ms
DB_IDLE_TIMEOUT=600000                  # Idle timeout in ms
DB_MAX_LIFETIME=1800000                 # Max connection lifetime
DB_LEAK_DETECTION=60000                 # Leak detection threshold

# Logging Levels (Optional)
FILTER_LOG_LEVEL=INFO                   # Diagnostic filter logging
INTERCEPTOR_LOG_LEVEL=INFO              # Request interceptor logging
EXCEPTION_LOG_LEVEL=INFO               # Exception handler logging
SQL_LOG_LEVEL=INFO                      # Hibernate SQL logging
SENTRY_LOG_LEVEL=INFO                   # Sentry integration logging

# Admin Reset (WARNING: Use only for recovery)
FORCE_ADMIN_RESET=false                  # Set to 'true' to reset admin (doesn't work on Railway)
```

## Monitoring & Observability

### Prometheus Metrics (Comprehensive)

#### Business Metrics
- `chicken.calculator.calculations.total` - Total calculations by location
- `chicken.calculator.marination.total` - Marination operations by type and location
- `chicken.calculator.sales_data.total` - Sales data operations by operation and location
- `chicken.calculator.location_access.total` - Location-specific access counts
- `chicken.calculator.locations.active` - Active locations gauge
- `chicken.calculator.sales_records.total` - Total sales records gauge
- `chicken.calculator.marination_records.total` - Total marination records gauge

#### Performance Metrics
- `chicken.calculator.calculations.duration` - Calculation timing (timer)
- `chicken.calculator.database.duration` - Database operation timing
- `chicken.calculator.location.lookup.duration` - Location lookup timing
- `http_server_requests` - Standard Spring Boot metrics
- `hikaricp.*` - Database connection pool metrics

#### Custom Metrics API
- `MetricsService.recordCalculation()` - Track calculations with location
- `MetricsService.recordMarination()` - Track marination operations
- `MetricsService.recordSalesData()` - Track sales operations
- `MetricsService.trackTime()` - Generic timing wrapper
- Dynamic gauge registration for business metrics

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
- **Framework**: JUnit 5 + Mockito-Kotlin 5.1.0 + Spring Boot Test
- **Integration**: TestContainers 1.19.0 for PostgreSQL
- **API Testing**: RestAssured with Kotlin extensions
- **Current Coverage**: ~30% (⚠️ tests broken - see Known Issues)
- **Target Coverage**: 80%
- **Test Utilities**: Comprehensive `TestBase.kt` with factory methods
- **Test Data**: Complete factories for all entities

### ⚠️ CRITICAL: Test Configuration Broken
- **Issue**: Invalid `spring.profiles.active` in `application-test.yml`
- **Fix**: Remove line 3 from `application-test.yml`
- **Impact**: All Spring Boot tests fail to start

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

# Start backend with debug tools
cd backend && mvn spring-boot:run -Dspring.profiles.active=dev

# Start admin portal
cd admin-portal && npm start

# Start frontend
cd frontend && npm start

# Quick development (Windows)
.\run-dev.bat              # Starts with dev profile and env vars
.\run-dev-test.bat         # Starts with test configuration
.\test-profile-isolation.bat  # Tests profile-specific components
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

## Deployment Checklist

### Pre-Deployment Verification
```bash
# Verify backend compilation
cd backend
mvn clean compile         # Must succeed with no errors
mvn test-compile          # Must succeed with no errors

# Verify frontend builds
cd ../admin-portal && npm run build
cd ../frontend && npm run build
```

### Generate Secure Secrets
```bash
# Generate JWT Secret (minimum 32 characters)
openssl rand -base64 48

# Generate Admin Password (must meet complexity requirements)
# - At least 8 characters
# - Contains uppercase, lowercase, and numbers
```

### Post-Deployment Verification

#### Health Checks
- `/api/health` returns UP status
- `/actuator/health` shows component health
- Database connection verified

#### Security Verification
- Admin login requires password change on first use
- CSRF tokens working (check browser DevTools)
- JWT stored in httpOnly cookies (not sessionStorage)
- H2 console inaccessible (`/h2-console` returns 403)

#### Monitoring
- `/actuator/prometheus` accessible
- Metrics being collected
- Sentry receiving error reports (trigger test error)
- Correlation IDs in response headers

#### Multi-Tenant Functionality
- Create test location via admin portal
- Access location via slug URL
- Verify data isolation between locations
- Test sales data and marination logs per location

#### Accessibility & Mobile
- Mobile navigation hamburger menu works
- Forms have proper ARIA labels
- Color contrast meets WCAG standards
- Touch targets are 44px minimum

#### API Versioning
- `/api/v1/*` endpoints working
- Legacy `/api/*` endpoints still functional
- Frontend using new versioned endpoints
- Marination calculation using MarinationRequest (NOT ChickenCalculationRequest)
- POST `/api/v1/calculator/calculate` returns CalculationResult

### First Admin Login

1. Navigate to `/admin`
2. Login with `admin@yourcompany.com` and configured password
3. **You will be forced to change password**
4. Set new secure password
5. Access admin dashboard

## Known Issues & Bugs

### ✅ RESOLVED: Admin Password Change Feature
- **Status**: Fixed (December 12, 2024)
- **Issue**: Admin password change now works correctly
- **Solution**: AdminService uses injected PasswordEncoder bean

### Current Known Issues

#### ⚠️ Test Configuration Broken
- **File**: `backend/src/test/resources/application-test.yml:3`
- **Issue**: Invalid `spring.profiles.active: test` in profile-specific resource
- **Fix**: Remove line 3, use `@ActiveProfiles("test")` annotation instead
- **Impact**: All Spring Boot tests fail to start

#### ⚠️ AdminService Test Failing
- **File**: `AdminServiceTest.kt`
- **Issue**: Missing `@Mock PasswordEncoder` field
- **Fix**: Add mock field for password encoder
- **Impact**: 5 test methods failing

#### ⚠️ Frontend Test Environment Missing
- **Issue**: `jest-environment-jsdom` not installed
- **Fix**: `npm install --save-dev jest-environment-jsdom`
- **Impact**: Frontend tests cannot run

## Troubleshooting Guide

### 🔴 CRITICAL: Servlet 500 Errors (Current Production Issue)

#### Symptoms
- All custom endpoints return 500 errors
- Controllers execute successfully (reach return statement)
- Exception occurs AFTER controller returns
- Actuator endpoints work fine

#### What's Been Ruled Out
- ❌ Write-after-commit (AfterCommitGuardFilter found no violations)
- ❌ Missing converters (Jackson present and configured)
- ❌ Filter chain issues (single doFilter() calls implemented)
- ❌ Sentry interference (disabled)
- ❌ Path pattern issues (fixed)

#### Next Investigation Steps
1. Check controller return types and @ResponseBody usage
2. Review WebConfig.kt for issues
3. Test with minimal Spring Boot defaults
4. Verify ResponseEntity usage in controllers

#### Diagnostic Tools Available
```bash
# Check filter order and converters at startup
grep "Registered OncePerRequestFilter" logs
grep "MVC MESSAGE CONVERTERS" logs

# Monitor for write-after-commit
grep "AfterCommitGuard" logs

# Check response lifecycle
grep "ResponseProbe" logs
```

### Common Issues

#### Spring 6 Path Pattern Restrictions
- **Symptom**: PatternParseException with /** patterns
- **Cause**: Spring 6's PathPatternParser doesn't allow /** wildcards
- **Solution**: Use explicit path lists or custom RequestMatchers
- **Pattern**: See "Spring 6 Compatibility Requirements" section

#### Servlet Exception with @Controller and Resources
- **Symptom**: 500 errors with "Servlet.service() threw exception"
- **Cause**: Spring's Resource handling conflicts with servlet processing
- **Solution**: Use @RestController for all REST endpoints
- **Note**: @Controller should only be used for view resolution

#### Compilation Errors
- **Micrometer API Type Mismatch**
  - Symptom: `MeterFilter.commonTags()` errors
  - Solution: Use `List<Tag>` with `Tag.of(key, value)` instead of varargs

- **Test Entity Construction**
  - Symptom: Location/SalesData constructor errors
  - Solution: Include all required fields (managerName, managerEmail for Location)

- **MarinationRequest Structure**
  - Symptom: Type mismatch for inventory/projectedSales
  - Solution: Use nested InventoryData and ProjectedSales objects, not primitives

#### JWT Token Issues
- **Symptom**: 401 errors after deployment
- **Solution**: Ensure JWT_SECRET is set (32+ chars)

#### Database Connection
- **Symptom**: Application won't start
- **Solution**: Verify DATABASE_URL format and credentials
- **Note**: Railway provides DATABASE_URL automatically for PostgreSQL

#### Admin Password Reset
- **Symptom**: Cannot login after password change
- **Solution**: Run V4 migration or create new migration to DELETE FROM admin_users
- **Note**: FORCE_ADMIN_RESET env var doesn't work properly on Railway

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

## Recent Compilation Fixes (December 12, 2024)

### Critical Fixes Applied (26 errors resolved)

#### 1. Micrometer API Updates
- **MetricsConfig.kt**: Fixed `MeterFilter.commonTags()` to use `List<Tag>` with `Tag.of()`
- **MetricsService.kt**: Updated gauge registration, timer recording, and counter methods

#### 2. Sentry 7.0.0 Compatibility
- **SentryConfig.kt**: Updated initialization to use `Sentry.init { options -> }`
- Fixed Jakarta EE migration (javax.annotation → jakarta.annotation)
- Removed problematic SentryExceptionResolver bean

#### 3. Controller Enhancements
- **AdminPortalController.kt**: Added ResourceLoader for flexible static resource serving
- **AdminAuthController.kt**: Fixed missing return statement in logout method
- **ChickenCalculatorController.kt**: Fixed metrics recording with proper null handling

#### 4. Test Infrastructure Updates
- **Location Entity**: All tests updated with required fields (managerName, managerEmail)
- **SalesData Tests**: Updated to use BigDecimal for all monetary fields
- **MarinationLog Tests**: Fixed to match current entity structure
- **Deleted Obsolete Tests**: ChickenCalculatorServiceTest, ChickenCalculatorControllerTest

#### 5. Repository Enhancement
- **LocationRepository**: Added `existsBySlug()` method for test support

#### 6. PostgreSQL Migration Fixes (December 12, 2024)
- **DatabaseConfig.kt**: Custom configuration to handle Railway PostgreSQL URLs
  - Converts `postgresql://` to `jdbc:postgresql://`
  - Extracts credentials from embedded URLs
  - Uses Railway's PGUSER/PGPASSWORD env vars
- **FlywayConfig.kt**: Ensures migrations run before Hibernate validation
  - Uses injected DataSource bean
  - Configured with `@AutoConfigureBefore(HibernateJpaAutoConfiguration)`
- **Entity Generation**: Changed from IDENTITY to SEQUENCE for PostgreSQL
- **Flyway Migrations**: Added V3 for sequences, V4 for admin reset

## Recent Changes Log

### January 14, 2025 - Sentry Re-enabled
- **Analysis**: Sentry wasn't the cause of servlet errors (Spring 6 patterns were)
- **Upgrade**: Updated to Sentry 7.14.0 with Spring Boot 3 compatibility
- **Configuration**: Production-safe with noise filtering and conservative sampling
- **Features**: Error tracking, logging integration, performance monitoring
- **Result**: Full observability restored without impacting stability

### January 13, 2025 - Servlet 500 Errors RESOLVED
- **Root Cause**: Spring 6's PathPatternParser doesn't allow /** patterns
- **SpaController**: Replaced /** patterns with specific path mappings
- **SecurityConfig**: Replaced all /** patterns with custom RequestMatchers
- **Solution**: Used simple string matching instead of PathPatternParser
- **Result**: All endpoints now working correctly in production

### January 12, 2025 - Multi-Location Authentication System
- **Location Auth**: Password-protected location access with rate limiting
- **Session Isolation**: Location-specific JWT tokens (location_token_{slug})
- **Frontend Restructure**: Complete React app rewrite with LocationContext
- **Database**: V5 migration adding authentication fields to locations
- **Security**: BCrypt hashing, 15-minute lockout after 5 failed attempts
- **Controllers**: New LocationAuthController for location authentication
- **Services**: LocationAuthService for password and session management
- **Filters**: LocationAuthFilter for request validation
- **React Components**: LocationLogin, LocationLayout, RequireAuth, LandingPage
- **No Default Fallback**: Removed all default location logic from services

### December 2024 - PostgreSQL Migration & Production Updates
- **Database**: Successfully migrated from H2 to PostgreSQL on Railway
- **Security**: JWT in httpOnly cookies, CSRF protection, password change enforcement
- **Data**: Fixed multi-tenant isolation, added Flyway migrations (V1-V4)
- **Architecture**: Service layer, controller separation, global exception handling
- **Monitoring**: Prometheus metrics, correlation IDs, Sentry integration
- **Testing**: Backend/frontend test infrastructure
- **Accessibility**: WCAG 2.1 AA compliance, mobile navigation
- **Password Fix**: AdminService now uses injected PasswordEncoder (December 12, 2024)

### Files Modified
- 40+ files updated
- 4,677 lines added
- Complete security overhaul
- Architecture refactoring

## Spring 6 Compatibility Requirements

### Path Pattern Rules (CRITICAL)
Spring 6's PathPatternParser doesn't allow /** patterns. Use these approaches instead:

#### Controllers
```kotlin
// ❌ WRONG - Causes PatternParseException
@GetMapping("/admin/**")

// ✅ CORRECT - List specific paths
@GetMapping("/admin", "/admin/{path1}", "/admin/{path1}/{path2}")
```

#### Security Config
```kotlin
// ❌ WRONG - Will fail in Spring 6
.requestMatchers("/api/**").permitAll()

// ✅ CORRECT - Use custom RequestMatcher
val matcher = RequestMatcher { request ->
    request.servletPath.startsWith("/api/")
}
.requestMatchers(matcher).permitAll()
```

### Filter Implementation Rules
1. **Single doFilter() call**: Each filter must call chain.doFilter() exactly ONCE
2. **No write-after-commit**: Never modify response after it's committed
3. **Use @Profile("dev")**: Debug/diagnostic filters should be dev-only

## Advanced Business Logic Features (Undocumented)

### Marination Calculation Algorithm
- **Raw Chicken Distribution**: Proportional distribution when limited
- **Safety Factor**: Configurable safety margins in calculations
- **4-Day Optimization**: Considers full forecast period, not just daily
- **Pan Rounding**: 30% threshold-based rounding for practical use
- **Already-Marinated Handling**: Subtracts pre-marinated from requirements
- **Emergency Priority**: Day 0 (emergency) gets priority allocation

### Calculation Constants
- **Soy**: 16 pieces/pan, 15.6 kg yield/pan, 1 kg raw = 0.975 kg marinated
- **Teriyaki**: 13 pieces/pan, 12.675 kg yield/pan, 1 kg raw = 0.975 kg marinated
- **Turmeric**: 17 pieces/pan, 16.6 kg yield/pan, 1 kg raw = 0.976 kg marinated
- **Default portions/kg**: 9.5 (Soy), 9.5 (Teriyaki), 9.5 (Turmeric)

## Utility Scripts (Windows)

### Development Scripts
- `run-dev.bat` - Start with dev profile and environment variables
- `run-dev-test.bat` - Start with test configuration
- `test-profile-isolation.bat` - Validate profile-specific components
- `test-local-prod.bat` - Test production profile locally
- `start-app.ps1` - PowerShell development startup
- `start.sh` - Railway deployment script with file system debugging

## Important Notes for Claude Code Sessions

### ⚠️ Critical Information for Claude Code Sessions

#### Railway MCP Commands - Use These Exact IDs
```bash
# Critical IDs for Railway operations
Project ID: 767deec0-30ac-4238-a57b-305f5470b318
Service ID: fde8974b-10a3-4b70-b5f1-73c4c5cebbbe
Environment ID: f57580c2-24dc-4c4e-adf2-313399c855a9
Postgres ID: bbbadbce-026c-44f1-974c-00d5a457bccf
```

#### Production Status - FULLY OPERATIONAL ✅
- **Previous Issue**: Servlet 500 errors from /** patterns - RESOLVED
- **Solution Applied**: Removed all /** patterns for Spring 6 compatibility
- **Current State**: All endpoints working correctly
- **Monitoring**: Check /api/health and /probe/ok for status

#### Best Practices
1. **Controller Types**: Always use @RestController for REST endpoints
2. **Path Patterns**: Avoid /** patterns with Spring 6's PathPatternParser
3. **Security Config**: Use custom RequestMatchers for complex path matching
4. **Filter Chains**: Ensure single doFilter() call per filter
5. **Resource Handling**: Avoid returning Spring Resource types

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

## Latest Hardening Improvements (January 13, 2025)

### Security Enhancements
- **ResponseCookie Implementation**: Proper SameSite support via `Cookies.kt` utility
- **Security Headers**: CSP, X-Content-Type-Options, X-Frame-Options added
- **Cookie Configuration**: JWT_COOKIE_SAMESITE env var for flexible deployment

### Code Quality
- **Centralized Security Matchers**: Cleaner, more maintainable SecurityConfig
- **Regression Tests**: SpaControllerTest, SecurityConfigTest prevent future issues
- **Diagnostic Tools**: Comprehensive filter infrastructure (dev profile only)

### Key Files Added/Modified
- `security/Cookies.kt` - JWT cookie helper with SameSite
- `config/SecurityConfig.kt` - Centralized patterns, security headers
- `controller/SpaController.kt` - Spring 6 compatible patterns
- All debug controllers - Added @Profile("dev")
- All diagnostic filters - Added @Profile("dev")

---

*Last Updated: January 14, 2025 05:00 PST*  
*Production Status: 10/10 - FULLY OPERATIONAL & MONITORED ✅*  
*Servlet 500 Errors: RESOLVED with comprehensive hardening*  
*Sentry Monitoring: ACTIVE with production-safe configuration*  
*All endpoints working with enhanced security and observability*  
*Railway Service IDs documented above for MCP commands*