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

<!-- BROKER_MODE_KNOWN_ISSUE_START -->
### 4. Specialized Agents Cannot Access MCP
- **Issue**: Specialized agents (`dev-logs`, `dev-architect`, `config-doctor`, `test-generator`) cannot call MCP tools directly.
- **Root cause**: Current Claude Code sub-agent sandboxing; MCP servers not inherited in sub-agent context.
- **Workaround**: Use MCP Broker (main thread/general-purpose) to fetch data → save to `/ops/mcp/*` → pass file paths to specialists.
- **Status**: Broker Mode configured and validated (Aug 16, 2025).
<!-- BROKER_MODE_KNOWN_ISSUE_END -->

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
- Admin Portal Static Files (403) - RESOLVED (Jan 14, 2025)
- PatternParseException in SecurityConfig - FIXED (Aug 16, 2025) - Replaced invalid Spring 6 path patterns with safe forms
- Admin location creation "Access denied" - FIXED (Jan 14, 2025) - Added proper role-based authorization
- Security hardening sprint - COMPLETED (Aug 16, 2025) - All 9 security improvements implemented
- See `docs/archive/` for historical issues

## Getting Help
- Check `docs/testing-guide.md` for test setup
- Check `docs/deployment-guide.md` for deployment issues
- Check `docs/development-workflow.md` for local development
- Monitor Sentry for production errors