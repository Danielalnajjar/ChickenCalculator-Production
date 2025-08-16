# ChickenCalculator Production Guide

**Version**: 1.0.0 | **Status**: ✅ Fully Operational | **Updated**: August 16, 2025
**Last Session**: 2025-08-16 — Completed security hardening sprint (Steps A-I); fixed critical PatternParseException in Spring Security; branch feature/security-hardening ready for merge to main.

## Personal Developer Settings (Optional)
- @~/.claude/chicken-calculator-preferences.md

## Critical Instructions

### 🚨 ALWAYS Check Sentry First for Debugging
When debugging ANY issue, use Sentry MCP before investigating code:
- See @docs/sentry-integration.md for detailed guide

## Quick Access
- **Production**: https://chickencalculator-production-production-2953.up.railway.app
- **Admin**: /admin (admin@yourcompany.com)
- **Railway IDs & Credentials**: @docs/quick-reference.md

## Quick Commands
```bash
# Development
cd backend && mvn spring-boot:run -Dspring.profiles.active=dev
cd frontend && npm start
cd admin-portal && npm start

# Testing
cd backend && mvn test                    # Run backend tests
cd frontend && npm test                   # Run frontend tests
cd admin-portal && npm test               # Run admin portal tests
mvn jacoco:report                        # Generate coverage report

# Building
mvn clean compile                        # Verify backend compilation
mvn test-compile                         # Verify test compilation
cd frontend && npm run build             # Build frontend
cd admin-portal && npm run build         # Build admin portal

# Windows Development Scripts
.\run-dev.bat                           # Start with dev profile
.\run-dev-test.bat                      # Start with test config
.\test-profile-isolation.bat            # Test profile isolation
.\test-local-prod.bat                   # Test production locally

# Deployment
git push origin main  # Auto-deploys to Railway

# Pre-Deployment Verification
mvn clean compile && mvn test-compile    # Verify all code compiles
cd frontend && npm run build             # Verify frontend builds
cd admin-portal && npm run build         # Verify admin builds
curl https://chickencalculator-production-production-2953.up.railway.app/api/health

# Quick Health Checks
curl -s https://chickencalculator-production-production-2953.up.railway.app/api/health | jq
mcp__sentry__search_events "error count last 15 minutes"
mcp__railway__deployment_status "[latest deployment id]"

# Documentation Verification
/ud                                      # Verify docs follow best practices (uses ultrathink + opus)
```

## Project Context
- **Backend**: Spring Boot 3.2.0 + Kotlin 1.9.20
- **Frontend**: React 18.2.0 + TypeScript 4.9.5
- **Database**: PostgreSQL 16.8 on Railway
- **Monitoring**: Sentry 7.14.0 (wok-to-walk/java-spring-boot)
- **Test Coverage**: ~30% (needs improvement)

## Documentation
- @docs/quick-reference.md - URLs, IDs, credentials
- @docs/api-reference.md - API endpoints & structures
- @docs/development-workflow.md - Commands & setup
- @docs/deployment-guide.md - Railway deployment
- @docs/testing-guide.md - Test setup & issues
- @docs/architecture/overview.md - System architecture
- @docs/claude-code-patterns.md - Project-specific workflows
- @docs/location-auth-guide.md - Multi-tenant authentication
- @docs/troubleshooting.md - Common problems & solutions
- @docs/sentry-integration.md - Error monitoring guide
- @KNOWN_ISSUES.md - Active bugs & solutions
- @README.md - Project overview

<!-- BROKER_MODE_SPRINT_START -->
## Current Sprint Focus
1. ✅ Security hardening sprint completed (Steps A-I, Aug 16 2025)
2. **READY**: Create PR for feature/security-hardening → main merge
3. **NEXT**: Deploy security hardening to production
4. **FOLLOW-UP**: Fix test configuration issues (application-test.yml line 3)
5. **IMPROVEMENT**: Increase test coverage from ~30% to 80% target
<!-- BROKER_MODE_SPRINT_END -->

## Common MCP Commands
```bash
# Sentry Debugging
mcp__sentry__search_issues(organizationSlug: "wok-to-walk", naturalLanguageQuery: "recent errors last hour")
mcp__sentry__search_events(organizationSlug: "wok-to-walk", naturalLanguageQuery: "error count today")
mcp__sentry__get_issue_details(organizationSlug: "wok-to-walk", issueId: "[from search]")
mcp__sentry__analyze_issue_with_seer(organizationSlug: "wok-to-walk", issueId: "[from search]")

# Railway Deployment (Note: Requires Railway CLI authentication or use in Cursor)
mcp__railway__deployment_status(deploymentId: "[from deployment_list]")
mcp__railway__deployment_logs(deploymentId: "[from deployment_list]")
mcp__railway__service_restart(serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
mcp__railway__list_service_variables(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
```

## Code Conventions
### Spring Boot Patterns
- Use @RestController for REST endpoints (not @Controller + @ResponseBody)
- Constructor injection preferred over field injection
- Use @Transactional at service layer, not controller
- Return ResponseEntity for explicit status codes

### Kotlin Conventions
- Data classes for all DTOs and request/response objects
- Use val over var unless mutability required
- Named parameters for functions with 3+ parameters
- Extension functions for utility operations

### Error Handling
- Custom exceptions extend RuntimeException
- Global exception handler via @ControllerAdvice
- Always include correlation ID in error responses
- Log errors before throwing exceptions

### Database Patterns
- Repository interfaces extend JpaRepository
- Use @Query for complex queries with JPQL
- Pagination with Pageable for list endpoints
- Optimistic locking with @Version

## Common Error Patterns & Solutions

### Authentication Errors
- **JWT Token Expired (401)**: Check JWT_SECRET env var matches, verify token expiry time
- **Invalid Credentials (401)**: Verify ADMIN_DEFAULT_PASSWORD is set correctly
- **CSRF Token Mismatch (403)**: Ensure cookies are enabled, check SameSite policy
- **Session Timeout**: Check JWT cookie expiry, implement refresh token if needed

### Database Issues
- **Connection Pool Exhausted**: Increase DB_POOL_SIZE, check for connection leaks
- **Lock Timeout**: Review @Transactional boundaries, avoid long-running transactions
- **Migration Failed**: Check Flyway scripts syntax, verify PostgreSQL version compatibility
- **Constraint Violations**: Ensure unique constraints on location slugs, check cascade rules

### Railway Deployment Issues
- **Build Timeout**: Split build into stages, increase timeout, check memory limits
- **Port Binding Failed**: Always use PORT env var (8080 on Railway)
- **Environment Variable Missing**: Double-check Railway dashboard, use fallback defaults
- **Health Check Failing**: Ensure /api/health responds within 30s

### Frontend Issues
- **CORS Blocked**: Check allowed origins in SecurityConfig, verify proxy settings
- **Blank Page After Deploy**: Check PUBLIC_URL, verify routing configuration
- **API Calls Failing**: Ensure API_BASE_URL points to correct backend
- **Build Memory Error**: Increase Node memory with NODE_OPTIONS=--max-old-space-size=4096

<!-- BROKER_MODE_MCP_START -->
## MCP Server Configuration

### Project-Scoped MCP Servers (via `.mcp.json`)
- **Sentry** — error monitoring and debugging
- **Context7** — documentation and library references
- **Railway** — deployment management

### ⚠️ IMPORTANT MCP LIMITATIONS
- ✅ Main CLI & general-purpose agents: full MCP access
- ❌ Specialized agents (`dev-logs`, `dev-architect`, `config-doctor`, `test-generator`): **no direct MCP access**
- **Broker Mode**: main thread runs MCP and writes artifacts; specialists analyze artifacts only.

### New Workflow Pattern
```bash
# Main (MCP Broker) → writes artifacts under /ops/mcp/
# Router (macros) → orchestrates
# Specialists → analyze/edit/tests using artifacts

# Examples:
# Router:
SENTRY_ERRORS "wok-to-walk" "errors OR exceptions last 60 minutes"
RAILWAY_LOGS "<service-id>" 30
DOCS "spring boot" "Require ADMIN for /actuator/prometheus; remove CSRF ignore for /actuator/**"
```

**File References**
- `@.mcp.json` — project MCP configuration
- `@.env.example` — environment variable template
- `@.claude/agents/` — agent configurations and router macros
- `@ops/mcp/` — MCP artifacts (git-ignored)
<!-- BROKER_MODE_MCP_END -->

## Important Notes
- Password change required on first admin login
- Each location has independent authentication
- Multi-tenant data isolation enforced
- Use @RestController for REST endpoints (Spring 6)