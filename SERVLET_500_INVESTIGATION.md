# Servlet 500 Error Investigation

## Current Status: CRITICAL - UNRESOLVED
**Last Updated**: January 13, 2025  
**Branch**: fix/prod-mvc-converters-and-patterns (merged to main)  
**Production Impact**: ALL endpoints returning 500 errors

## Problem Summary

All REST endpoints in production return HTTP 500 Internal Server Error with the message:
```
Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception
```

Key characteristics:
- Works locally with dev profile
- Fails on Railway production environment
- No stack traces visible in logs
- Exception details are empty/null
- Affects ALL endpoints including simple ones like /api/health

## Investigation Timeline

### Phase 1: Initial Discovery (January 13, 2025)
- Discovered all custom endpoints returning 500 errors
- Actuator endpoints (/actuator/health) working fine
- Added TestController with simple endpoints - all fail

### Phase 2: Diagnostic Infrastructure (January 13, 2025)
Created multiple diagnostic components:

1. **ErrorTapFilter** (commit 79007ab)
   - Captures ERROR dispatch at HIGHEST_PRECEDENCE
   - Finding: ERROR_EXCEPTION attribute is null/empty
   - Exception happens but details not available

2. **TailLogFilter** 
   - Comprehensive request/response logging
   - Confirms requests reach Spring but fail in servlet

3. **FilterInventory**
   - Lists all registered filters at startup
   - Confirmed all filters load in correct order

### Phase 3: Sentry Investigation (January 13, 2025)
- Discovered Sentry was causing some servlet exceptions
- Disabled Sentry completely (removed SENTRY_DSN)
- Result: Some improvement but main issue persists

### Phase 4: Controller Type Investigation
- Changed all controllers to @RestController
- Created RootController for "/" path
- Created MinimalController with simplest possible endpoints
- Result: All still fail with same error

### Phase 5: MVC Converter Investigation (January 13, 2025)
**Hypothesis**: Missing or misconfigured HTTP message converters

**Actions taken**:
1. Created MvcDiagnostics to log converters at startup
2. Added DebugMvcController for runtime inspection
3. Added jackson-datatype-jsr310 dependency

**Findings**:
- Local: 10 converters including 3x MappingJackson2HttpMessageConverter
- Production: 9 converters including 2x MappingJackson2HttpMessageConverter
- Jackson ObjectMapper present with Kotlin module
- **Conclusion**: Converters ARE present - not the issue

### Phase 6: Path Pattern Investigation (January 13, 2025)
**Hypothesis**: Illegal path patterns in filters causing issues

**Error found**: "No more pattern data allowed after {*...} or ** pattern element"

**Actions taken**:
1. Removed ALL Ant-style patterns from JwtAuthenticationFilter
2. Simplified LocationAuthFilter path checks
3. Replaced with simple string operations (startsWith, equals)
4. Added PathUtil for normalized path handling

**Result**: Error message gone but 500s persist

## Current Evidence

### What We Know Works
- Application starts successfully
- Database connects properly
- All beans initialize
- Filters are registered correctly
- MVC converters are present
- Jackson ObjectMapper exists

### What Fails
- Every HTTP request to custom endpoints
- Even simplest endpoints (returning plain strings)
- Both GET and POST requests
- All content types (JSON, HTML, plain text)

### Error Pattern
```
1. Request arrives
2. Passes through security filters
3. Correlation ID assigned
4. Reaches dispatcherServlet
5. Exception thrown (no details)
6. ERROR dispatch triggered
7. Error page processing also fails
8. Generic 500 HTML error returned
```

## Environment Differences

### Local (Working)
- Direct execution via `mvn spring-boot:run`
- 10 HTTP message converters
- Dev profile with verbose logging
- No containerization

### Production (Failing)
- Railway containerized environment
- 9 HTTP message converters (one less)
- Production profile
- Behind Railway proxy/load balancer

## Code Locations

### Diagnostic Components
- `backend/src/main/kotlin/com/example/chickencalculator/filter/ErrorTapFilter.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/filter/TailLogFilter.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/config/FilterInventory.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/config/MvcDiagnostics.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/controller/DebugMvcController.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/util/PathUtil.kt`

### Test Controllers
- `backend/src/main/kotlin/com/example/chickencalculator/controller/TestController.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/controller/MinimalController.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/controller/RootController.kt`

## Testing Commands

### Local Testing (Works)
```bash
cd backend
set JWT_SECRET=test-secret-key-for-testing-purposes-only-1234567890
set ADMIN_DEFAULT_PASSWORD=Admin123!
mvn spring-boot:run -Dspring.profiles.active=dev

# Test endpoints
curl http://localhost:8080/minimal
curl http://localhost:8080/test-html
curl http://localhost:8080/api/health
```

### Production Testing (Fails)
```bash
curl https://chickencalculator-production-production-2953.up.railway.app/minimal
curl https://chickencalculator-production-production-2953.up.railway.app/test-html
curl https://chickencalculator-production-production-2953.up.railway.app/api/health
```

## Hypotheses to Investigate

### 1. Classpath/Dependency Conflict
- Something in Railway's container environment conflicts
- Different classloader behavior in container

### 2. Security Configuration
- Spring Security blocking requests in unexpected way
- CORS or CSRF issues specific to production

### 3. Proxy/Network Layer
- Railway's proxy modifying requests
- Header transformation causing issues

### 4. Resource Loading
- Static resources or templates not found
- Different file paths in container

### 5. Thread/Async Issues
- @EnableAsync causing problems
- Thread pool exhaustion

## Next Steps for Investigation

1. **Add more detailed exception capture**
   - Override ErrorAttributes to capture more details
   - Add custom HandlerExceptionResolver
   - Log at various points in request lifecycle

2. **Test without Spring Security**
   - Temporarily disable all security
   - See if requests succeed

3. **Minimal Spring Boot app**
   - Create bare minimum app with single endpoint
   - Deploy to Railway to isolate issue

4. **Check Railway-specific issues**
   - Review Railway forums for similar issues
   - Check if other Spring Boot apps have same problem

5. **Deep dive into dispatcherServlet**
   - Custom configuration
   - Handler mapping investigation
   - View resolver conflicts

## Railway Details

- **Project ID**: 767deec0-30ac-4238-a57b-305f5470b318
- **Service ID**: fde8974b-10a3-4b70-b5f1-73c4c5cebbbe
- **Environment ID**: f57580c2-24dc-4c4e-adf2-313399c855a9
- **URL**: https://chickencalculator-production-production-2953.up.railway.app

## Key Findings Summary

1. ✅ **NOT** a message converter issue
2. ✅ **NOT** a path pattern issue
3. ✅ **NOT** a compilation issue
4. ✅ **NOT** a Sentry issue (disabled)
5. ❓ **MIGHT BE** environment-specific
6. ❓ **MIGHT BE** security configuration
7. ❓ **MIGHT BE** resource loading issue

---

*For future Claude Code sessions: Start by reviewing ErrorTapFilter output and trying to capture the actual exception details that are currently missing.*