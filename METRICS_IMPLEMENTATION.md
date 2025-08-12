# Prometheus Metrics Integration - Implementation Guide

## Overview

This document describes the comprehensive Prometheus metrics integration implemented for the ChickenCalculator application. The implementation provides production-grade observability and monitoring capabilities.

## Implementation Summary

### 1. Dependencies Added

**Maven Dependencies (`pom.xml`):**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2. Configuration Updates

**Application Configuration (`application.yml`):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        step: 10s
        descriptions: true
    enable:
      jvm: true
      system: true
      process: true
      hikaricp: true
      web: true
    distribution:
      percentiles-histogram:
        "[http.server.requests]": true
        "[chicken.calculator.calculations]": true
      sla:
        "[http.server.requests]": 50ms, 100ms, 200ms, 500ms, 1s, 2s, 5s
        "[chicken.calculator.calculations]": 10ms, 25ms, 50ms, 100ms, 200ms
    tags:
      application: chicken-calculator
      environment: ${SPRING_PROFILES_ACTIVE:development}
```

**Security Configuration (`SecurityConfig.kt`):**
- Added `/actuator/prometheus` and `/actuator/metrics/**` to permitted endpoints
- No authentication required for metrics endpoints (suitable for monitoring systems)

### 3. Core Components

#### MetricsService.kt
A comprehensive service providing:

**Business Metrics:**
- `chicken.calculator.calculations.total` - Total calculations performed
- `chicken.calculator.marination.total` - Total marination operations
- `chicken.calculator.sales_data.total` - Sales data operations
- `chicken.calculator.location_access.total` - Location-specific accesses
- `chicken.calculator.locations.active` - Active locations gauge
- `chicken.calculator.sales.active_records` - Active sales records gauge
- `chicken.calculator.marination.active_records` - Active marination records gauge

**Performance Metrics:**
- `chicken.calculator.calculations.duration` - Calculation processing time
- `chicken.calculator.database.duration` - Database operation time
- `chicken.calculator.location.lookup.duration` - Location lookup time
- `chicken.calculator.operation.duration` - Generic operation timing

**Error Tracking:**
- `chicken.calculator.errors.total` - Total errors by type and operation
- `chicken.calculator.auth.failures.total` - Authentication failures
- `chicken.calculator.errors.by_location.total` - Location-specific errors

**Multi-tenant Metrics:**
- Location-specific counters and timers
- Per-location operation tracking
- Location access patterns

**Admin Metrics:**
- `chicken.calculator.admin.operations.total` - Admin operations
- `chicken.calculator.admin.operations.duration` - Admin operation timing

**Health Metrics:**
- `chicken.calculator.health.checks.total` - Health check results
- `chicken.calculator.health.checks.duration` - Health check timing

#### MetricsConfig.kt
Configuration class providing:
- `@Timed` annotation processing via `TimedAspect`
- Common tags for all metrics
- Metric filtering and renaming
- Environment-specific tagging

### 4. Controller Updates

All major controllers updated with:
- `@Timed` annotations for automatic timing
- MetricsService integration for custom metrics
- Error tracking and performance monitoring
- Location-specific metric recording

**Updated Controllers:**
- `ChickenCalculatorController` - Calculation metrics
- `SalesDataController` - Sales data metrics
- `MarinationLogController` - Marination metrics
- `AdminLocationController` - Admin operation metrics
- `LocationSlugController` - Location access metrics
- `AdminAuthController` - Authentication metrics
- `HealthController` - Health check metrics

## Available Metrics Endpoints

### Prometheus Metrics
- **URL**: `/actuator/prometheus`
- **Format**: Prometheus text format
- **Authentication**: None required
- **Purpose**: Scraping by Prometheus server

### JSON Metrics
- **URL**: `/actuator/metrics`
- **Format**: JSON
- **Authentication**: None required
- **Purpose**: Human-readable metrics inspection

### Specific Metric Details
- **URL**: `/actuator/metrics/{metric.name}`
- **Format**: JSON with detailed information
- **Example**: `/actuator/metrics/chicken.calculator.calculations.total`

## Key Features

### 1. Business Intelligence
- Track calculation patterns per location
- Monitor user engagement across locations
- Sales and marination operation tracking
- Location performance comparison

### 2. Performance Monitoring
- Response time distribution with percentiles
- Database operation timing
- Location lookup performance
- Admin operation efficiency

### 3. Error Tracking
- Categorized error metrics by type and operation
- Location-specific error rates
- Authentication failure patterns
- Health check failure tracking

### 4. Multi-tenant Observability
- Per-location metrics isolation
- Location access pattern analysis
- Tenant-specific performance monitoring
- Location lifecycle tracking

### 5. Production Readiness
- JVM and system metrics
- HikariCP connection pool monitoring
- HTTP request metrics with SLA tracking
- Environment and deployment tagging

## Sample Metrics Output

```
# HELP chicken_calculator_calculations_total Total number of chicken calculations performed
# TYPE chicken_calculator_calculations_total counter
chicken_calculator_calculations_total{application="chicken-calculator",environment="production",type="calculation"} 1547.0

# HELP chicken_calculator_calculations_duration Time taken for chicken calculations
# TYPE chicken_calculator_calculations_duration histogram
chicken_calculator_calculations_duration_bucket{application="chicken-calculator",environment="production",operation="calculate",le="0.01"} 892.0
chicken_calculator_calculations_duration_bucket{application="chicken-calculator",environment="production",operation="calculate",le="0.025"} 1234.0
chicken_calculator_calculations_duration_bucket{application="chicken-calculator",environment="production",operation="calculate",le="0.05"} 1456.0
chicken_calculator_calculations_duration_bucket{application="chicken-calculator",environment="production",operation="calculate",le="0.1"} 1523.0
chicken_calculator_calculations_duration_bucket{application="chicken-calculator",environment="production",operation="calculate",le="0.2"} 1547.0
chicken_calculator_calculations_duration_bucket{application="chicken-calculator",environment="production",operation="calculate",le="+Inf"} 1547.0
chicken_calculator_calculations_duration_count{application="chicken-calculator",environment="production",operation="calculate"} 1547.0
chicken_calculator_calculations_duration_sum{application="chicken-calculator",environment="production",operation="calculate"} 45.789

# HELP chicken_calculator_location_operations_total Operations per location
# TYPE chicken_calculator_location_operations_total counter
chicken_calculator_location_operations_total{application="chicken-calculator",environment="production",location="fashion-show",operation="calculations"} 234.0
chicken_calculator_location_operations_total{application="chicken-calculator",environment="production",location="downtown-store",operation="calculations"} 567.0
```

## Monitoring and Alerting Recommendations

### 1. Key Metrics to Monitor
- Error rate: `rate(chicken_calculator_errors_total[5m])`
- Response time p95: `histogram_quantile(0.95, chicken_calculator_calculations_duration_bucket)`
- Location availability: `chicken_calculator_locations_active`
- Authentication failures: `rate(chicken_calculator_auth_failures_total[5m])`

### 2. Suggested Alerts
- High error rate (>5% over 5 minutes)
- Slow response times (p95 > 500ms)
- Authentication failure spikes
- Database health check failures

### 3. Dashboard Panels
- Business metrics: calculations per location, user activity
- Performance: response time distributions, throughput
- Errors: error rates by type and location
- Infrastructure: JVM metrics, connection pools

## Deployment Notes

### Railway Configuration
The metrics endpoints are configured to work with Railway's single-port constraint:
- All metrics available on port 8080
- No additional ports required
- Prometheus can scrape `/actuator/prometheus` directly

### Security Considerations
- Metrics endpoints are public (no authentication required)
- No sensitive data exposed in metrics
- Consider restricting access via Railway networking if needed

### Performance Impact
- Minimal overhead from metrics collection
- Efficient memory usage with metric filtering
- Async metric recording where possible

## Troubleshooting

### Common Issues
1. **Metrics not appearing**: Check if `micrometer-registry-prometheus` dependency is present
2. **@Timed not working**: Ensure `TimedAspect` bean is configured
3. **High memory usage**: Review metric filters and retention policies

### Debugging
- Check `/actuator/metrics` for available metrics
- Use `/actuator/health` to verify application status
- Monitor logs for MetricsService error messages

## Future Enhancements

### Potential Additions
1. Custom business dashboards
2. Anomaly detection on calculation patterns
3. Cost tracking per location
4. Capacity planning metrics
5. Customer behavior analytics

### Integration Opportunities
1. Grafana dashboards
2. PagerDuty alerting
3. Slack notifications
4. Custom monitoring tools
5. Business intelligence platforms

This implementation provides a solid foundation for comprehensive monitoring and observability of the ChickenCalculator application in production environments.