# Troubleshooting Guide

## Quick Fixes for Common Issues

### 🔴 Application Won't Start

#### Spring Boot Fails to Start
```bash
# Check for port conflicts
netstat -an | findstr :8080

# Kill process using port
taskkill /PID [process-id] /F

# Verify environment variables
echo %JWT_SECRET%
echo %ADMIN_DEFAULT_PASSWORD%

# Run with explicit profile
mvn spring-boot:run -Dspring.profiles.active=dev
```

#### Tests Won't Run
```bash
# Fix test configuration
# Remove line 3 from backend/src/test/resources/application-test.yml
# Line to remove: spring.profiles.active: test

# Fix missing test environment
cd admin-portal && npm install --save-dev jest-environment-jsdom
cd ../frontend && npm install --save-dev jest-environment-jsdom
```

### 🔴 Authentication Issues

#### Admin Can't Login
```bash
# Check admin password is set
mcp__railway__list_service_variables(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")

# Update if missing
mcp__railway__variable_set(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9", name: "ADMIN_DEFAULT_PASSWORD", value: "SecurePassword123!")

# Restart service
mcp__railway__service_restart(serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
```

#### JWT Token Issues
```bash
# Verify JWT_SECRET is at least 32 characters
# Generate new secret if needed
openssl rand -base64 48

# Update on Railway
mcp__railway__variable_set(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9", name: "JWT_SECRET", value: "[new-secret]")
```

### 🔴 Database Connection Issues

#### Local Development
```bash
# H2 database should work automatically
# If not, check application-dev.yml
# Should contain: spring.datasource.url: jdbc:h2:mem:testdb
```

#### Production
```bash
# Check DATABASE_URL is set
mcp__railway__list_service_variables(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")

# Railway should provide this automatically
# Format: postgresql://user:password@host:port/database
```

### 🔴 Deployment Failures

#### Build Fails on Railway
```bash
# Check compilation locally first
mvn clean compile
mvn test-compile

# Check for syntax errors
mvn dependency:tree

# Verify Java version
# Railway needs Java 17+
```

#### Frontend Build Fails
```bash
# Clear npm cache
npm cache clean --force

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Build locally to test
npm run build
```

### 🔴 Production Errors

#### 500 Internal Server Error
```bash
# Check Sentry immediately
mcp__sentry__search_issues(organizationSlug: "wok-to-walk", naturalLanguageQuery: "500 errors last hour")

# Get detailed analysis
mcp__sentry__analyze_issue_with_seer(organizationSlug: "wok-to-walk", issueId: "[from search]")

# Check deployment logs
mcp__railway__deployment_logs(deploymentId: "[latest]")
```

#### Slow Performance
```bash
# Check database pool
# Look for: HikariPool-1 - Connection is not available
mcp__railway__deployment_logs(deploymentId: "[latest]", limit: 100)

# Monitor metrics
curl https://chickencalculator-production-production-2953.up.railway.app/actuator/prometheus | grep hikari
```

### 🔴 CORS Issues

#### Frontend Can't Connect to Backend
```bash
# Verify CORS configuration in SecurityConfig.kt
# Should allow your frontend URL

# For local development
# Backend: http://localhost:8080
# Frontend: http://localhost:3000
# Admin: http://localhost:3001
```

### 🔴 Flyway Migration Issues

#### Migration Checksum Mismatch
```bash
# Only in development - NEVER in production
# Add to application-dev.yml
spring.flyway.validate-on-migrate: false

# Or clean database (DEVELOPMENT ONLY)
mvn flyway:clean
```

#### Migration Fails
```bash
# Check migration naming
# Format: V{number}__{description}.sql
# Example: V6__Add_new_table.sql

# Test locally first
mvn spring-boot:run -Dspring.profiles.active=dev
```

### 🔴 Sentry Not Reporting

#### No Errors in Sentry
```bash
# Verify SENTRY_DSN is set
mcp__railway__list_service_variables(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")

# Check Sentry initialization in logs
mcp__railway__deployment_logs(deploymentId: "[latest]", limit: 50)
# Look for: "Sentry initialized"
```

### 🔴 Location Issues

#### Can't Create Location
```bash
# Must be logged in as admin
# Check admin token in browser DevTools
# Cookie name: admin_token

# Via curl
curl -X POST https://chickencalculator-production-production-2953.up.railway.app/api/v1/admin/locations \
  -H "Cookie: admin_token=[token]" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","slug":"test"}'
```

## Emergency Procedures

### Immediate Rollback
```bash
# Find last working deployment
mcp__railway__deployment_list(projectId: "767deec0-30ac-4238-a57b-305f5470b318", serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9", limit: 10)

# Redeploy via Railway dashboard
# Or revert via Git
git revert HEAD && git push origin main
```

### Service Restart
```bash
mcp__railway__service_restart(serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
```

### Check All Systems
```bash
# 1. Health check
curl https://chickencalculator-production-production-2953.up.railway.app/api/health

# 2. Error count
mcp__sentry__search_events(organizationSlug: "wok-to-walk", naturalLanguageQuery: "error count last hour")

# 3. Recent deployments
mcp__railway__deployment_list(projectId: "767deec0-30ac-4238-a57b-305f5470b318", serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9", limit: 3)
```

## Getting Help

1. **Check Sentry First**: Most issues are already logged
2. **Review KNOWN_ISSUES.md**: Known problems and solutions
3. **Check deployment logs**: Recent changes that might cause issues
4. **Use claude-code-patterns.md**: Common workflow solutions