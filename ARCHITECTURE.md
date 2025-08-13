# ChickenCalculator System Architecture

## Overview

The ChickenCalculator is a production-ready, multi-tenant restaurant management system built with modern architecture principles, comprehensive security, and enterprise-grade monitoring.

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
│  • Connection: HikariCP pool (5-10 connections)         │
│  • SSL/TLS: Enforced for production                     │
│  • Migrations: V1-V4 applied                            │
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
1. **V1__initial_schema.sql**: Core tables with PostgreSQL sequences
2. **V2__add_password_change_required.sql**: Password policy support
3. **V3__add_marination_defaults.sql**: Default values for marination
4. **V4__reset_admin_password.sql**: Admin recovery mechanism
5. **V5__add_location_authentication.sql**: Location auth fields and indexes

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
- **Sentry Integration**: Real-time error monitoring
- **User Context**: Error tracking with user information
- **Performance Monitoring**: Transaction tracing
- **Environment Tagging**: dev/staging/production

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

### Technology Roadmap
- **Kubernetes Migration**: Container orchestration
- **Distributed Tracing**: OpenTelemetry integration
- **API Gateway**: Centralized API management
- **Message Queue**: Asynchronous processing

---

*Architecture Documentation - January 2025*
*Production Ready System (10/10)*