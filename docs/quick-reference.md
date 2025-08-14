# Quick Reference

## Production URLs
- **Production**: https://chickencalculator-production-production-2953.up.railway.app
- **Admin Portal**: /admin
- **Location Access**: /{slug} (password protected)
  - Calculator: /{slug}/calculator
  - Sales Data: /{slug}/sales
  - Marination History: /{slug}/history
- **Metrics**: /actuator/prometheus
- **Health**: /api/health

## Railway IDs (for MCP Commands)
```bash
Project ID: 767deec0-30ac-4238-a57b-305f5470b318
Service ID: fde8974b-10a3-4b70-b5f1-73c4c5cebbbe
Environment ID: f57580c2-24dc-4c4e-adf2-313399c855a9
Postgres ID: bbbadbce-026c-44f1-974c-00d5a457bccf
```

## Common Test Commands
```bash
# Backend Tests
cd backend && mvn test                    # Run all tests
mvn test -Dtest=AdminServiceTest         # Run specific test
mvn test-compile                         # Verify compilation
mvn jacoco:report                        # Generate coverage report

# Frontend Tests
cd frontend && npm test                  # Run tests
npm run test:coverage                    # With coverage
npm test -- --watchAll=false            # CI mode

# Admin Portal Tests
cd admin-portal && npm test              # Run tests
npm run test:coverage                    # With coverage

# Windows Scripts
.\run-dev-test.bat                       # Test profile isolation
.\test-local-prod.bat                    # Production profile test
```

## Frequently Used MCP Patterns
```bash
# Quick Health Check Pattern
mcp__sentry__search_events(organizationSlug: "wok-to-walk", naturalLanguageQuery: "error count last 15 minutes")
curl https://chickencalculator-production-production-2953.up.railway.app/api/health

# Debug Issue Pattern
mcp__sentry__search_issues(organizationSlug: "wok-to-walk", naturalLanguageQuery: "recent errors")
mcp__sentry__analyze_issue_with_seer(organizationSlug: "wok-to-walk", issueId: "[ID]")
mcp__railway__deployment_logs(deploymentId: "[latest]")

# Deploy & Monitor Pattern
git push origin main
mcp__railway__deployment_status(deploymentId: "[latest]")
# Wait 2-3 minutes
mcp__sentry__search_issues(organizationSlug: "wok-to-walk", naturalLanguageQuery: "new errors last 5 minutes")

# Environment Update Pattern
mcp__railway__list_service_variables(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
mcp__railway__variable_set(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9", name: "VAR", value: "value")
mcp__railway__service_restart(serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
```

## Default Credentials
- **Admin**: admin@yourcompany.com
  - Password: Set via ADMIN_DEFAULT_PASSWORD env var
  - Note: Password change required on first login
- **Locations**: Each has own password
  - Default: "ChangeMe123!" (V5 migration)
  - Admins can generate/update passwords

## GitHub Repository
https://github.com/Danielalnajjar/ChickenCalculator-Production

## Deployment
- Platform: Railway (auto-deploy from main branch)
- Port: 8080 (Railway single-port constraint)
- Database: PostgreSQL 16.8

## Sentry Configuration
- **Organization**: wok-to-walk
- **Project**: java-spring-boot
- **Region URL**: https://us.sentry.io
- **DSN**: Configured in application.yml