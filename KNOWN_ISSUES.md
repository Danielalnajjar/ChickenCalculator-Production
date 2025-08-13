# Known Issues - ChickenCalculator Production

**Last Updated**: January 13, 2025  
**Production Status**: Fully Operational ✅  
**Test Status**: Configuration Issues ⚠️

## Current Issues

### 1. Backend Test Configuration Broken
**Status**: Active  
**Severity**: High (for development)  
**File**: `backend/src/test/resources/application-test.yml:3`  
**Issue**: Invalid `spring.profiles.active: test` in profile-specific resource  
**Error**: `Property 'spring.profiles.active' imported from location 'class path resource [application-test.yml]' is invalid in a profile specific resource`  
**Solution**: 
```yaml
# Remove line 3 from application-test.yml
# Use @ActiveProfiles("test") annotation in test classes instead
```
**Impact**: All Spring Boot tests fail to start

### 2. AdminService Test Dependency Injection Failing
**Status**: Active  
**Severity**: Medium  
**File**: `AdminServiceTest.kt`  
**Issue**: `@InjectMocks` cannot instantiate AdminService due to missing PasswordEncoder mock  
**Solution**: 
```kotlin
// Add to AdminServiceTest.kt
@Mock
private lateinit var passwordEncoder: PasswordEncoder
```
**Impact**: 5 test methods failing

### 3. Frontend Test Environment Missing
**Status**: Active  
**Severity**: Medium  
**Location**: `admin-portal/jest.config.js`  
**Issues**:
- `jest-environment-jsdom` package not installed (required for Jest 28+)
- Invalid `moduleNameMapping` configuration option (should be `moduleNameMapper`)  
**Solution**: 
```bash
cd admin-portal
npm install --save-dev jest-environment-jsdom
# Then fix jest.config.js: rename moduleNameMapping to moduleNameMapper
```
**Impact**: Frontend tests cannot run

### 4. Railway Environment Variables
**Status**: Active  
**Severity**: Low  
- `FORCE_ADMIN_RESET` env var doesn't work on Railway
- Use database migrations instead for admin resets
- This is a minor limitation that has a documented workaround

---

## ✅ Resolved Issues

### ✅ Servlet 500 Errors in Production
**Status**: RESOLVED  
**Fixed**: January 13, 2025 02:45 PST  
**Severity**: Was Critical  

#### Problem
All custom REST endpoints were failing with HTTP 500 after controllers successfully processed requests. The exception occurred in Spring MVC post-processing due to Spring 6's PathPatternParser not allowing /** wildcard patterns.

#### Root Cause
Spring 6 introduced breaking changes to PathPatternParser that disallow patterns after `/**`. The application had several instances of these patterns:
- `@GetMapping("/admin/**")` in SpaController
- `@GetMapping("/location/{slug}/**")` in SpaController  
- `.requestMatchers("/api/**")` in SecurityConfig
- Multiple CSRF ignore patterns with `/**`

#### Solution
Replaced all /** patterns with:
1. **Controllers**: Listed specific paths explicitly
   ```kotlin
   @GetMapping("/admin", "/admin/login", "/admin/dashboard")
   ```
2. **Security Config**: Used custom RequestMatcher objects
   ```kotlin
   val matcher = RequestMatcher { request ->
       request.servletPath.startsWith("/api/")
   }
   ```

#### Verification
All endpoints now working correctly in production. Tested:
- `/api/health` ✅
- `/test` ✅  
- `/probe/ok` ✅
- All admin endpoints ✅
- All location endpoints ✅

See [SERVLET_500_INVESTIGATION.md](SERVLET_500_INVESTIGATION.md) for complete investigation history.

### ✅ Admin Password Change Feature
**Status**: RESOLVED  
**Fixed**: December 12, 2024  
**Solution**: AdminService properly uses injected PasswordEncoder bean

### ✅ Compilation Errors
**Status**: RESOLVED  
**Fixed**: December 12, 2024  
**Details**: Fixed 26 compilation errors including:
- Micrometer API compatibility
- Sentry 7.0.0 updates
- Test entity construction
- MarinationRequest structure

### ✅ PostgreSQL Migration
**Status**: RESOLVED  
**Fixed**: December 12, 2024  
**Details**: Successfully migrated from H2 to PostgreSQL on Railway
- Custom DatabaseConfig for Railway URLs
- Flyway migrations V1-V5 applied
- Sequences configured for PostgreSQL

### ✅ Multi-Location Authentication
**Status**: RESOLVED  
**Fixed**: January 12, 2025  
**Details**: Complete location-based authentication system
- Password-protected location access
- Session isolation per location
- Rate limiting (5 attempts, 15-minute lockout)

---

## Known Limitations

### Railway Platform Constraints
- Single port exposure (8080 only)
- Memory limits during build process
- Environment variable requirements before deployment
- No interactive terminal for debugging

### Development vs Production Differences
- Different number of HTTP message converters (not an issue)
- Exception stack traces limited in production logs
- Containerized environment affects some configurations

### Spring 6 Breaking Changes
- PathPatternParser doesn't allow /** patterns
- Requires specific path mappings or custom matchers
- More strict about path pattern syntax

---

## Workarounds & Mitigations

### For Railway Environment Variables
1. Use Flyway migrations for database changes
2. Set environment variables through Railway dashboard
3. Use DATABASE_URL format provided by Railway

### For Spring 6 Path Patterns
1. List paths explicitly in controllers
2. Use custom RequestMatcher for complex patterns
3. Avoid /** in all Spring configurations

### For Development
1. Use dev profile with verbose logging
2. Test with `run-dev-test.bat` for consistent environment
3. Always test path patterns locally before deployment

---

## Contributing to Issue Resolution

If you encounter new issues:

1. **Check diagnostic tools**:
   - Correlation IDs in headers
   - `/api/health` endpoint
   - Railway dashboard logs

2. **Key Railway IDs**:
   - Project: `767deec0-30ac-4238-a57b-305f5470b318`
   - Service: `fde8974b-10a3-4b70-b5f1-73c4c5cebbbe`
   - Environment: `f57580c2-24dc-4c4e-adf2-313399c855a9`

3. **Testing Commands**:
   ```bash
   # Local testing
   cd backend && mvn spring-boot:run -Dspring.profiles.active=dev
   
   # Test endpoints
   curl http://localhost:8080/api/health
   curl http://localhost:8080/test
   curl http://localhost:8080/probe/ok
   ```

4. **Production Testing**:
   ```bash
   curl https://chickencalculator-production-production-2953.up.railway.app/api/health
   ```

---

*Last Updated: January 13, 2025 11:00 PST*  
*Production: Fully operational ✅*  
*Development: Test configuration needs fixes ⚠️*  
*See [CLAUDE.md](CLAUDE.md#testing-infrastructure) for comprehensive testing information*