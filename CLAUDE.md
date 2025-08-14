# ChickenCalculator Production Guide

**Version**: 1.0.0 | **Status**: ✅ Fully Operational | **Updated**: January 14, 2025

## Personal Developer Settings (Optional)
- @~/.claude/chicken-calculator-preferences.md

## Critical Instructions

### 🚨 ALWAYS Check Sentry First for Debugging
When debugging ANY issue, use Sentry MCP before investigating code:
- @docs/sentry-integration.md

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
- @KNOWN_ISSUES.md - Active bugs & solutions
- @README.md - Project overview

## Current Sprint Focus
1. Fix test configuration (application-test.yml line 3)
2. Improve test coverage to 80%
3. Monitor Sentry for production issues

## Common MCP Commands
```bash
# Sentry Debugging
mcp__sentry__search_issues(organizationSlug: "wok-to-walk", naturalLanguageQuery: "recent errors last hour")
mcp__sentry__search_events(organizationSlug: "wok-to-walk", naturalLanguageQuery: "error count today")
mcp__sentry__get_issue_details(organizationSlug: "wok-to-walk", issueId: "[from search]")
mcp__sentry__analyze_issue_with_seer(organizationSlug: "wok-to-walk", issueId: "[from search]")

# Railway Deployment
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

## MCP Server Configuration for Sub-Agents

### Project-Level Configuration
The project includes a `.mcp.json` file that configures MCP servers for all agents:
- **Railway**: Deployment management using `@railway/mcp-server` (with API token)
- **Sentry**: Error monitoring and debugging
- **Context7**: Documentation and library references

### User-Level Configuration
MCP servers are also configured globally in user settings for consistent access across projects.

### Railway MCP Server (Updated Jan 14, 2025)
- Now using official Railway MCP server: `@railway/mcp-server`
- Configured at both user and project levels
- Installation: `npx -y @railway/mcp-server`
- Requires Railway CLI to be installed

### Testing MCP Access
Sub-agents launched via the Task tool will automatically inherit MCP configurations from:
1. Project-level `.mcp.json` (shared team configuration)
2. User-level settings (personal global configuration)

### Troubleshooting
- If MCP servers are not accessible, run `claude mcp list` to verify configuration
- For Railway access issues, ensure RAILWAY_API_TOKEN is set in `.env` or `.mcp.json`
- Reset project MCP choices with `claude mcp reset-project-choices` if needed
- Railway MCP may require Railway CLI authentication

## Important Notes
- Password change required on first admin login
- Each location has independent authentication
- Multi-tenant data isolation enforced
- Use @RestController for REST endpoints (Spring 6)