# Known Issues

## Critical Issues to Fix

### 1. Test Configuration Broken ⚠️
- **File**: `backend/src/test/resources/application-test.yml:3`
- **Issue**: Invalid `spring.profiles.active: test` in profile-specific resource
- **Fix**: Remove line 3 from application-test.yml
- **Impact**: All Spring Boot tests fail to start
- See `docs/testing-guide.md` for full testing issues

### 2. Frontend Test Environment Missing
- **Issue**: `jest-environment-jsdom` not installed
- **Fix**: `npm install --save-dev jest-environment-jsdom`
- **Impact**: Frontend tests cannot run

### 3. Test Coverage Too Low
- **Current**: ~30% backend, ~20% frontend
- **Target**: 80% coverage
- **Priority**: High

## Non-Critical Issues

### Environment Variables
- `FORCE_ADMIN_RESET` doesn't work on Railway (use migrations instead)
- See `docs/deployment-guide.md` for environment setup

### Performance
- Consider upgrading TypeScript to 5.x
- Consider upgrading jjwt to 0.12.x
- Database pool could be optimized

## Resolved Issues ✅
- Admin password change - FIXED (Dec 12, 2024)
- Servlet 500 errors - RESOLVED (Jan 13, 2025)
- Multi-location auth - IMPLEMENTED (Jan 12, 2025)
- PostgreSQL migration - COMPLETED (Dec 2024)
- See `docs/archive/` for historical issues

## Getting Help
- Check `docs/testing-guide.md` for test setup
- Check `docs/deployment-guide.md` for deployment issues
- Check `docs/development-workflow.md` for local development
- Monitor Sentry for production errors