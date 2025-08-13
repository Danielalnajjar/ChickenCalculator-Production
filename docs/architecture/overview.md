# ChickenCalculator System Architecture

**Last Updated**: January 13, 2025  
**Version**: 1.0.0  
**Status**: Production Ready (10/10) ✅

## Overview

The ChickenCalculator is a production-ready, multi-tenant restaurant management system built with modern architecture principles, comprehensive security, and enterprise-grade monitoring. The system uses Spring Boot 3.2.0 (Spring 6) with Kotlin 1.9.20 for the backend and React 18.2.0 with TypeScript for dual frontend applications.

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Railway Platform                      │
│                      (PORT 8080)                         │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                 Spring Boot Application                  │
│                    (Kotlin 1.9.20)                       │
├─────────────────────────────────────────────────────────┤
│                    Presentation Layer                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │              REST Controllers (v1)                │  │
│  │  • AdminAuthController    • AdminLocationController │  │
│  │  • LocationAuthController • ChickenCalculatorController │  │
│  │  • SalesDataController    • MarinationLogController│  │
│  │  • LocationSlugController • HealthController      │  │
│  │  • RootController         • AdminPortalController │  │
│  │  • TestController                                  │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Business Layer                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │                Service Components                 │  │
│  │  • LocationManagementService • LocationAuthService│  │
│  │  • SalesDataService      • MarinationLogService   │  │
│  │  • AdminService          • ChickenCalculatorService│  │
│  │  • MetricsService        • JwtService             │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Security & Middleware                │  │
│  │  • JwtAuthenticationFilter • LocationAuthFilter   │  │
│  │  • CorrelationIdFilter    • RequestLoggingInterceptor│  │
│  │  • GlobalExceptionHandler • CSRF Protection       │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                     Data Access Layer                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │              JPA Repositories                     │  │
│  │  • LocationRepository     • AdminUserRepository   │  │
│  │  • SalesDataRepository   • MarinationLogRepository│  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL 16.8 on Railway                  │
│                   (Flyway Migrations)                    │
│  • Connection: HikariCP pool (10 connections)           │
│  • SSL/TLS: Enforced for production                     │
│  • Migrations: V1-V5 applied (Jan 13, 2025)            │
└─────────────────────────────────────────────────────────┘
```

## Frontend Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Browser/Client                         │
├─────────────────────────────────────────────────────────┤
│                 Admin Portal (React 18)                  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • Authentication (httpOnly cookies)              │  │
│  │  • CSRF Protection                                │  │
│  │  • Location Management Dashboard                  │  │
│  │  • Responsive Design (Mobile + Desktop)           │  │
│  │  • WCAG 2.1 AA Compliance                        │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│              Main Calculator App (React)                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • Password-Protected Locations (/{slug})         │  │
│  │  • LocationContext for Authentication State       │  │
│  │  • Marination Calculator                          │  │
│  │  • Sales Data Management                          │  │
│  │  • Multi-tenant Data Isolation                    │  │
│  │  • Session-Isolated JWT Tokens                    │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Security Architecture

### Authentication & Authorization

#### Admin Authentication
- **BCrypt Password Hashing**: 10 rounds for admin passwords
- **JWT Tokens**: Stored in httpOnly cookies (admin_token)
- **Password Policy**: Mandatory change on first login
- **Session Management**: Secure cookie configuration

#### Location Authentication
- **Per-Location Passwords**: Each location has independent auth
- **BCrypt Hashing**: Secure password storage for locations
- **JWT Tokens**: Location-specific cookies (location_token_{slug})
- **Rate Limiting**: 5 failed attempts triggers 15-minute lockout
- **Session Timeout**: 8-hour default, configurable per location
- **CSRF Protection**: Double-submit cookie pattern

### Security Layers
1. **Transport Security**: HTTPS enforcement
2. **Application Security**: Spring Security framework
3. **Data Security**: Parameterized queries, input validation
4. **Infrastructure Security**: Environment variable secrets

## Multi-Tenant Architecture

### Tenant Isolation Strategy
```
Request Flow:
1. Client Request → /{slug}
2. LocationAuthFilter validates JWT token
3. Location authentication verified
4. Location context propagated to services
5. Service layer enforces tenant boundaries (no default fallback)
6. Repository queries scoped to location
7. Response filtered by location context
```

### Data Isolation
- **Row-Level Security**: Each record has location_id
- **Service Layer Enforcement**: Business logic validates tenant access
- **Query Scoping**: Repository methods filter by location
- **Cross-Tenant Protection**: Security checks prevent data leakage

## Database Design

### PostgreSQL Configuration
```yaml
Database Platform: PostgreSQL 16.8
Connection Pool: HikariCP
Pool Size: 10 (Railway optimized)
Min Idle: 2
Max Lifetime: 900000ms (15 minutes)
Connection Timeout: 20000ms
SSL Mode: require
```

### Connection Handling (Railway-Specific)
```kotlin
// DatabaseConfig.kt handles Railway's PostgreSQL URLs
// Supports both formats:
// - postgresql://user:pass@host:port/db
// - jdbc:postgresql://host:port/db
// Extracts embedded credentials and uses PGUSER/PGPASSWORD
```

### Schema (Managed by Flyway)
```sql
-- Core Tables
admin_users (
  id, email, password_hash, name, 
  password_change_required, created_at, updated_at
)

locations (
  id, name, slug, manager_name, manager_email,
  password_hash, requires_auth, session_timeout_hours,
  last_password_change, failed_login_attempts,
  last_failed_login, is_default, status, 
  created_at, updated_at
)

sales_data (
  id, location_id, date, sold_soy, sold_teriyaki,
  sold_turmeric, created_at, updated_at
)

marination_log (
  id, location_id, date, timestamp, type,
  raw_weight, pan_quantity, already_marinated,
  is_end_of_day, created_at, updated_at
)
```

### Migration Strategy
- **Version Control**: Flyway migrations (V1, V2, V3, V4)
- **PostgreSQL Sequences**: entity_id_seq for all tables
- **Rollback Support**: Down migrations for each version
- **Data Integrity**: Foreign key constraints
- **Performance**: Optimized indexes on common queries

### Applied Migrations
1. **V1__initial_schema.sql**: Core tables and initial structure
2. **V2__add_indexes_and_constraints.sql**: Performance and data integrity
3. **V3__postgresql_sequences.sql**: PostgreSQL sequence support for IDs
4. **V4__reset_admin_password.sql**: Admin recovery mechanism
5. **V5__add_location_authentication.sql**: Location auth fields and indexes (Jan 12, 2025)

## Diagnostic Infrastructure (Dev Profile Only)

### Purpose
Comprehensive diagnostic tools added during servlet 500 investigation, now restricted to development profile only for debugging future issues.

### Filter Execution Order (Production)
```
1. errorTapFilter            - ERROR dispatch capture (HIGHEST_PRECEDENCE)
2. characterEncodingFilter   - Spring Boot default
3. responseProbeFilter       - Response lifecycle monitoring
4. formContentFilter         - Spring Boot default
5. requestContextFilter      - Spring Boot default
6. afterCommitGuardFilter    - Write-after-commit detection
7. locationAuthFilter        - Location authentication (single doFilter)
8. jwtAuthenticationFilter   - Admin JWT auth (single doFilter)
9. tailLogFilter            - Request/response logging
```

### Diagnostic Components (All @Profile("dev"))

#### Error Detection Filters
- **ErrorTapFilter** - Captures ERROR dispatch at HIGHEST_PRECEDENCE
- **AfterCommitGuardFilter** - Detects write-after-commit violations (LOWEST_PRECEDENCE - 1)
- **ResponseProbeFilter** - Monitors response lifecycle (position 3)
- **TailLogFilter** - Comprehensive request/response logging (LOWEST_PRECEDENCE)

#### Error Handling Components
- **TappingErrorAttributes** - Captures Spring error attributes (@Primary)
- **PlainErrorController** - Plain text error responses

#### Diagnostic Utilities
- **FilterInventory** - Documents filter registration order at startup
- **MvcDiagnostics** - Logs HTTP message converters at startup
- **MappingsLogger** - Logs all registered request mappings
- **PathUtil** - Normalizes request paths (production utility)

#### Debug Controllers
- **TestController** - Testing endpoints (/test, /test-html)
- **MinimalController** - Basic functionality (/minimal)
- **DebugController** - Spring mapping inspection (/debug/mappings)
- **DebugMvcController** - HTTP converter debugging (/debug/converters)
- **ProbeController** - Health probing (/probe/ok, /probe/boom)

### Filter Implementation Requirements

#### Path Pattern Rules (Spring 6 Breaking Change)
**CRITICAL**: Spring 6's PathPatternParser doesn't allow /** patterns:
```kotlin
// CORRECT - Use specific paths or custom matchers
@GetMapping("/admin", "/admin/login", "/admin/dashboard")
val matcher = RequestMatcher { request -> 
    request.servletPath.startsWith("/api/")
}

// WRONG - Causes PatternParseException in Spring 6
@GetMapping("/admin/**")  // PatternParseException!
.requestMatchers("/api/**")  // PatternParseException!
```

**Solution**: Replace all /** patterns with:
1. Specific path lists for controllers
2. Custom RequestMatcher objects for security config
3. Simple string operations in filters

#### Filter Chain Rules
**CRITICAL**: Each filter must call chain.doFilter() exactly ONCE:
```kotlin
// CORRECT - Single exit point
override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
    // All logic here
    chain.doFilter(req, res) // Only ONE call at the end
}

// WRONG - Multiple calls cause write-after-commit
if (condition) {
    chain.doFilter(req, res)
    return
}
// ... more logic ...
chain.doFilter(req, res)
```

## Controller Architecture Best Practices

### Controller Type Selection
- **@RestController**: Use for all REST API endpoints returning data
  - Automatically applies @ResponseBody to all methods
  - Returns JSON/XML directly without view resolution
  - Examples: AdminAuthController, ChickenCalculatorController, RootController
  
- **@Controller**: Use ONLY for serving static files or view templates
  - Requires @ResponseBody for data responses (can cause issues)
  - Used for Spring MVC view resolution
  - Example: LocationSlugController (serves FileSystemResource)

### Common Pitfalls to Avoid
1. **Never** use @Controller with Resource return types and @ResponseBody
2. **Avoid** returning Spring Resource objects in REST endpoints
3. **Always** use @RestController for JSON/HTML string responses
4. **Be careful** with GlobalExceptionHandler - can mask real issues

## API Design

### RESTful Principles
- **Resource-Based**: /api/v1/{resource}
- **HTTP Methods**: GET, POST, PUT, DELETE
- **Status Codes**: Proper HTTP status codes
- **Error Handling**: Standardized error responses

### Current API Structure
- **Main Calculation**: POST /api/v1/calculator/calculate
  - Uses `MarinationRequest` with nested `InventoryData` and `ProjectedSales`
  - Returns `CalculationResult` with marination suggestions
  - Service method: `calculateMarination()` (NOT calculateChickenRequirements)

### API Versioning
- **URL Versioning**: /api/v1/* prefix
- **Backward Compatibility**: Legacy /api/* still supported
- **Migration Path**: Clear upgrade path for clients

## Monitoring & Observability

### Metrics Collection
```
Prometheus Metrics:
├── Business Metrics
│   ├── calculations_total{location}
│   ├── marination_operations{type,location}
│   └── sales_data_operations{operation,location}
├── Performance Metrics
│   ├── http_server_requests{method,uri,status}
│   ├── response_time_percentiles{p50,p95,p99}
│   └── db_connection_pool{active,idle}
└── Error Metrics
    ├── errors_total{type,location}
    └── authentication_failures{reason}
```

### Logging Architecture
- **Structured Logging**: JSON format with logstash encoder
- **Correlation IDs**: Request tracing across services
- **Log Levels**: Environment-specific configuration
- **Sensitive Data**: Automatic exclusion of passwords/tokens

### Error Tracking
- **Sentry Integration**: Version 7.14.0 (ACTIVE in production as of January 14, 2025)
  - **Configuration**: Production-safe with comprehensive filtering
  - **Noise Filtering**: Health checks, actuator, and static resources excluded
  - **Sampling**: 10% traces, 1% profiles for cost-effective monitoring
  - **Context**: Multi-tenant location tracking (non-PII)
  - **Integration**: sentry-spring-boot-starter-jakarta + sentry-logback
- **Complementary Approaches**: 
  - GlobalExceptionHandler handles 18 exception types
  - Correlation IDs for request tracing
  - Structured JSON logging with logstash encoder
  - Business metrics tracking via Micrometer
- **Debug Tools**: Comprehensive diagnostic filters available in dev profile

## Deployment Architecture

### Container Strategy
```dockerfile
# Multi-stage build
1. Maven Build Stage → JAR artifact
2. Node Build Stage → React bundles
3. Runtime Stage → Optimized image
```

### Railway Deployment
- **Auto-Deploy**: GitHub push triggers deployment
- **PostgreSQL Service**: Managed database with auto-backup
- **Environment Variables**: Secure secret management
  - DATABASE_URL auto-injected by Railway
  - PGUSER, PGPASSWORD, PGHOST managed by platform
- **Health Checks**: Automatic monitoring
- **Rollback**: Quick rollback capability
- **Single Port**: All traffic through port 8080

## Performance Optimizations

### Backend
- **Connection Pooling**: HikariCP with 10 connections (Railway optimized)
- **Transaction Management**: @Transactional with isolation levels
- **Query Optimization**: PostgreSQL-specific indexes
- **Lazy Loading**: JPA fetch strategies
- **Sequence Generation**: PostgreSQL sequences for IDs
- **Date Functions**: CAST(timestamp AS DATE) for PostgreSQL compatibility

### Frontend
- **Code Splitting**: Lazy loading of routes
- **Bundle Optimization**: Webpack production builds
- **Asset Caching**: Static resource optimization
- **Responsive Images**: Optimized for different screens

## Testing Architecture

### Test Pyramid
```
         E2E Tests
        /    |    \
       Integration Tests
      /      |      \
     Unit Tests (Base)
```

### Test Infrastructure
- **Backend**: JUnit 5, Mockito, Spring Boot Test
- **Frontend**: Jest, React Testing Library
- **Database**: TestContainers for integration tests
- **Coverage**: 70% threshold (target: 80%)

## Scalability Considerations

### Horizontal Scaling
- **Stateless Design**: JWT tokens, no server sessions
- **Database Pooling**: Connection management
- **Load Balancing**: Railway platform support

### Vertical Scaling
- **JVM Tuning**: Memory and GC optimization
- **Database Optimization**: Query performance
- **Caching Strategy**: Future Redis integration

## Security Compliance

### Standards
- **OWASP Top 10**: Protection against common vulnerabilities
- **WCAG 2.1 AA**: Accessibility compliance
- **GDPR Ready**: Data protection considerations

### Security Features
- **Input Validation**: All user inputs validated
- **SQL Injection Protection**: Parameterized queries
- **XSS Protection**: httpOnly cookies, content security policy
- **CSRF Protection**: Double-submit cookies

## Future Architecture Enhancements

### Planned Improvements
1. **Redis Caching**: Performance optimization
2. **Rate Limiting**: API protection
3. **Service Mesh**: Microservices migration path
4. **Event Sourcing**: Audit trail enhancement
5. **GraphQL API**: Alternative API interface

### Technology Stack Details

### Backend Stack
- **Spring Boot**: 3.2.0 (Spring 6, Jakarta EE)
- **Kotlin**: 1.9.20 with Spring/JPA plugins
- **Java**: 17+ required
- **JWT**: jjwt 0.11.5 (consider upgrading to 0.12.x)
- **Database**: PostgreSQL 16.8 + Flyway 10.4.0
- **Monitoring**: Micrometer + Prometheus + Sentry 7.14.0 (Active)
- **Testing**: JUnit 5 + Mockito-Kotlin 5.1.0 + TestContainers 1.19.0

### Frontend Stack
- **React**: 18.2.0 + React DOM
- **TypeScript**: 4.9.5 (consider upgrading to 5.x)
- **Routing**: React Router 6.18.0
- **HTTP**: Axios 1.6.0
- **UI**: TailwindCSS 3.3.5 + Heroicons 2.0.18
- **Build**: Webpack 5.89.0 (custom configuration)
- **Testing**: Jest 29.7.0 + React Testing Library 13.4.0

## Technology Roadmap
- **Immediate**: Fix test configuration issues
- **Short-term**: Upgrade TypeScript to 5.x, JJWT to 0.12.x
- **Medium-term**: Expand test coverage to 80%
- **Long-term**: 
  - Kubernetes Migration: Container orchestration
  - Distributed Tracing: OpenTelemetry integration
  - API Gateway: Centralized API management
  - Message Queue: Asynchronous processing
  - Redis Caching: Performance optimization

---

*Architecture Documentation - January 2025*
*Production Ready System (10/10)*