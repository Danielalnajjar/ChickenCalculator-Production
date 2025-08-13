# Servlet 500 Error Investigation

## Current Status: CRITICAL - UNRESOLVED
**Last Updated**: January 13, 2025 01:05 PST  
**Latest Investigation**: Write-after-commit hypothesis RULED OUT  
**Production Impact**: ALL custom endpoints returning 500 errors  

## Problem Summary

All custom REST endpoints in production return HTTP 500 Internal Server Error. The exception occurs AFTER controllers successfully process requests (status=200) but BEFORE the response is sent to the client.

### Key Characteristics
- Works locally with dev profile
- Fails on Railway production environment  
- Actuator endpoints (/actuator/health) work correctly
- Controllers execute successfully (return 200)
- Exception occurs in Spring MVC post-processing
- No exception details captured in logs

## Investigation Timeline

### Phase 1: Initial Discovery (January 13, 2025)
- Discovered all custom endpoints returning 500 errors
- Actuator endpoints working fine
- Added TestController with simple endpoints - all fail

### Phase 2: Diagnostic Infrastructure (January 13, 2025 - commit 79007ab)
Created diagnostic components:

1. **ErrorTapFilter** 
   - Captures ERROR dispatch at HIGHEST_PRECEDENCE
   - Finding: ERROR_EXCEPTION attribute is null/empty
   
2. **TailLogFilter**
   - Comprehensive request/response logging
   - Confirms requests reach Spring but fail in servlet
   
3. **FilterInventory**
   - Lists all registered filters at startup
   - Confirmed filter load order

### Phase 3: Sentry Investigation (January 13, 2025)
- Discovered Sentry causing some servlet exceptions
- Disabled Sentry completely (removed SENTRY_DSN)
- Result: Some improvement but main issue persists

### Phase 4: Controller Type Investigation
- Changed all controllers to @RestController
- Created RootController for "/" path
- Created MinimalController with simplest possible endpoints
- Result: All still fail with same error

### Phase 5: MVC Converter Investigation (January 13, 2025)
**Hypothesis**: Missing or misconfigured HTTP message converters

**Actions**:
1. Created MvcDiagnostics to log converters at startup
2. Added DebugMvcController for runtime inspection  
3. Added jackson-datatype-jsr310 dependency

**Findings**:
- Local: 10 converters
- Production: 9 converters (both have Jackson)
- **Conclusion**: Converters present - NOT the issue

### Phase 6: Path Pattern Investigation (January 13, 2025 - commit e461114)
**Hypothesis**: Illegal path patterns in filters

**Actions**:
1. Removed ALL Ant-style patterns from filters
2. Replaced with simple string operations
3. Added PathUtil for normalized path handling

**Result**: Pattern errors gone but 500s persist

### Phase 7: Force Error to Surface (January 13, 2025 - commit 2ee2d0c)
**Hypothesis**: Exception not being captured properly

**Actions**:
1. **TappingErrorAttributes** - Capture actual Throwable Spring uses
   - Result: NOT logging any exceptions
   
2. **PlainErrorController** - Bypass JSON conversion  
   - Result: NOT being invoked
   
3. **ResponseProbeFilter** - Monitor response lifecycle
   - Result: Shows status=200 BEFORE exception

**Key Finding**: Response shows 200, then exception occurs, suggesting issue in post-processing

### Phase 8: Write-After-Commit Investigation (January 13, 2025 - commit 9274e69)
**Hypothesis**: Multiple filterChain.doFilter() calls causing write-after-commit

**Actions**:
1. **AfterCommitGuardFilter** - Trap any post-commit writes
   - Result: NO violations detected
   
2. **Fixed JwtAuthenticationFilter** - Eliminated double chain calls
   - Previously called chain.doFilter() twice
   - Now calls exactly once
   
3. **Fixed LocationAuthFilter** - Consolidated 8 chain calls to 1
   - Previously had multiple exit points
   - Now single chain call at end

**CONCLUSION**: NOT a write-after-commit issue - AfterCommitGuardFilter found no violations

## Current Evidence

### What Works
✅ Application starts successfully  
✅ Database connects properly  
✅ All beans initialize  
✅ Filters registered correctly  
✅ MVC converters present  
✅ Jackson ObjectMapper exists  
✅ Actuator endpoints respond correctly  
✅ Controllers execute successfully (reach return statement)  

### What Fails
❌ Response delivery for custom endpoints  
❌ Exception happens AFTER controller returns  
❌ Spring MVC post-processing of controller response  
❌ Error page processing also fails  

### Error Pattern
```
1. Request arrives
2. Passes through security filters ✅
3. Correlation ID assigned ✅
4. Controller processes request ✅
5. Controller returns (status 200) ✅
6. [EXCEPTION OCCURS HERE] ❌
7. ERROR dispatch triggered
8. Error attributes empty (no exception attached)
9. Error page processing fails
10. Generic 500 HTML returned
```

## Diagnostic Tools Added

### Filters (in execution order)
1. **errorTapFilter** - Captures ERROR dispatch
2. **characterEncodingFilter** - Spring default
3. **responseProbeFilter** - Monitors response mutations
4. **formContentFilter** - Spring default  
5. **requestContextFilter** - Spring default
6. **afterCommitGuardFilter** - Detects write-after-commit
7. **locationAuthFilter** - Location authentication
8. **jwtAuthenticationFilter** - Admin JWT auth
9. **tailLogFilter** - Request/response logging

### Error Handling Components
- **TappingErrorAttributes** - Captures Spring error attributes
- **PlainErrorController** - Plain text error responses
- **MvcDiagnostics** - Logs converters at startup
- **FilterInventory** - Documents filter order
- **PathUtil** - Normalizes request paths

## Environment Differences

### Local (Working)
- Direct execution via `mvn spring-boot:run`
- 10 HTTP message converters
- Dev profile with verbose logging
- No containerization

### Production (Failing)  
- Railway containerized environment
- 9 HTTP message converters
- Production profile
- Behind Railway proxy/load balancer

## What We've Ruled Out

❌ **Write-after-commit** - AfterCommitGuardFilter found no violations  
❌ **Missing converters** - Jackson present and configured  
❌ **Path pattern issues** - All fixed, using simple string ops  
❌ **Sentry interference** - Completely disabled  
❌ **Controller type issues** - Using @RestController everywhere  
❌ **Filter chain issues** - Single chain.doFilter() calls  
❌ **CORS/Security** - Actuator endpoints work with same config  

## Next Investigation Steps

### Most Likely Causes (Based on Evidence)

1. **View Resolution Issue**
   - Controllers may be returning values Spring tries to resolve as views
   - Check @ResponseBody annotations
   - Verify ResponseEntity usage

2. **Response Type Mismatch**
   - Spring may be failing to serialize response objects
   - Check what controllers are actually returning
   - Verify return type compatibility

3. **Railway-Specific Environment Issue**  
   - Something in containerized environment
   - Missing classpath resources
   - Different Spring Boot auto-configuration

4. **Spring MVC Configuration**
   - WebMvcConfigurer interference
   - Missing/conflicting @EnableWebMvc
   - Handler mapping issues

### Immediate Actions for Next Session

1. **Examine Controller Return Types**
   ```kotlin
   // Check all controllers for:
   - Missing @ResponseBody (if using @Controller)
   - Return type issues
   - View name resolution attempts
   ```

2. **Add Controller Diagnostics**
   ```kotlin
   @RestController
   class DiagnosticController {
       @GetMapping("/diag/simple")
       fun simple(): ResponseEntity<String> {
           return ResponseEntity.ok("test")
       }
       
       @GetMapping("/diag/json")
       fun json(): ResponseEntity<Map<String, String>> {
           return ResponseEntity.ok(mapOf("status" to "ok"))
       }
   }
   ```

3. **Check WebMvcConfigurer**
   - Review WebConfig.kt for issues
   - Temporarily disable custom configuration
   - Test with minimal Spring Boot defaults

## Railway Details

**CRITICAL FOR FUTURE SESSIONS**:
- **Project ID**: `767deec0-30ac-4238-a57b-305f5470b318`
- **Service ID**: `fde8974b-10a3-4b70-b5f1-73c4c5cebbbe`
- **Environment ID**: `f57580c2-24dc-4c4e-adf2-313399c855a9`
- **URL**: https://chickencalculator-production-production-2953.up.railway.app

## Testing Commands

### Local Testing (Works)
```bash
cd backend
set JWT_SECRET=test-secret-key-for-testing-purposes-only-1234567890
set ADMIN_DEFAULT_PASSWORD=Admin123!
mvn spring-boot:run -Dspring.profiles.active=dev

# Test endpoints
curl http://localhost:8080/minimal
curl http://localhost:8080/test
curl http://localhost:8080/api/health
```

### Production Testing (Fails)
```bash
# These all return 500:
curl https://chickencalculator-production-production-2953.up.railway.app/minimal
curl https://chickencalculator-production-production-2953.up.railway.app/test
curl https://chickencalculator-production-production-2953.up.railway.app/api/health

# This works:
curl https://chickencalculator-production-production-2953.up.railway.app/actuator/health
```

## Key Code Locations

### Diagnostic Components
- `backend/src/main/kotlin/com/example/chickencalculator/filter/ErrorTapFilter.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/filter/TailLogFilter.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/filter/ResponseProbeFilter.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/filter/AfterCommitGuardFilter.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/error/TappingErrorAttributes.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/error/PlainErrorController.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/config/FilterInventory.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/config/MvcDiagnostics.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/util/PathUtil.kt`

### Test Controllers
- `backend/src/main/kotlin/com/example/chickencalculator/controller/TestController.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/controller/MinimalController.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/controller/RootController.kt`

## Summary for Next Session

The servlet 500 errors occur AFTER controllers successfully process requests but BEFORE responses are sent. We've ruled out filter issues, converter problems, and write-after-commit violations. The issue appears to be in Spring MVC's post-processing of controller responses, possibly related to view resolution or response type handling. Focus investigation on what controllers are returning and how Spring is trying to process those return values.

---

*For future Claude Code sessions: Start by examining controller return types and Spring MVC configuration. The diagnostic infrastructure is in place and working.*