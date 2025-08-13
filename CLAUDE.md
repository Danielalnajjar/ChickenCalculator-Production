# ChickenCalculator Production Guide

**Version**: 1.0.0 | **Status**: ✅ Fully Operational | **Updated**: January 14, 2025

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

# Deployment
git push origin main  # Auto-deploys to Railway
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

## Important Notes
- Password change required on first admin login
- Each location has independent authentication
- Multi-tenant data isolation enforced
- Use @RestController for REST endpoints (Spring 6)