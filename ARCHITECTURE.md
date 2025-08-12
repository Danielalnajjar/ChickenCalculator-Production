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
│  │  • ChickenCalculatorController                    │  │
│  │  • SalesDataController    • MarinationLogController│  │
│  │  • LocationSlugController • HealthController      │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Business Layer                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │                Service Components                 │  │
│  │  • LocationManagementService                      │  │
│  │  • SalesDataService      • MarinationLogService   │  │
│  │  • AdminService          • ChickenCalculatorService│  │
│  │  • MetricsService        • JwtService             │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                    │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Security & Middleware                │  │
│  │  • JwtAuthenticationFilter                        │  │
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
│                    PostgreSQL Database                   │
│                   (Flyway Migrations)                    │
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
│  │  • Location-based Access (/{slug})                │  │
│  │  • Marination Calculator                          │  │
│  │  • Sales Data Management                          │  │
│  │  • Multi-tenant Data Isolation                    │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Security Architecture

### Authentication & Authorization
- **BCrypt Password Hashing**: 10 rounds for secure password storage
- **JWT Tokens**: Stored in httpOnly cookies (XSS-safe)
- **CSRF Protection**: Double-submit cookie pattern
- **Password Policy**: Mandatory change on first login
- **Session Management**: Secure cookie configuration

### Security Layers
1. **Transport Security**: HTTPS enforcement
2. **Application Security**: Spring Security framework
3. **Data Security**: Parameterized queries, input validation
4. **Infrastructure Security**: Environment variable secrets

## Multi-Tenant Architecture

### Tenant Isolation Strategy
```
Request Flow:
1. Client Request → /{slug} or X-Location-Id header
2. LocationSlugController validates slug
3. Location context propagated to services
4. Service layer enforces tenant boundaries
5. Repository queries scoped to location
6. Response filtered by location context
```

### Data Isolation
- **Row-Level Security**: Each record has location_id
- **Service Layer Enforcement**: Business logic validates tenant access
- **Query Scoping**: Repository methods filter by location
- **Cross-Tenant Protection**: Security checks prevent data leakage

## Database Design

### Schema (Managed by Flyway)
```sql
-- Core Tables
admin_users (
  id, email, password_hash, name, 
  password_change_required, created_at, updated_at
)

locations (
  id, name, slug, manager_name, manager_email,
  is_default, status, created_at, updated_at
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
- **Version Control**: Flyway migrations (V1, V2, V3...)
- **Rollback Support**: Down migrations for each version
- **Data Integrity**: Foreign key constraints
- **Performance**: Optimized indexes on common queries

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
- **Environment Variables**: Secure secret management
- **Health Checks**: Automatic monitoring
- **Rollback**: Quick rollback capability

## Performance Optimizations

### Backend
- **Connection Pooling**: HikariCP with 5-8 connections
- **Transaction Management**: @Transactional boundaries
- **Query Optimization**: Indexed database queries
- **Lazy Loading**: JPA fetch strategies

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

*Architecture Documentation - December 2024*
*Production Ready System (9.5/10)*