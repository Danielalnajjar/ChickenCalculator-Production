# Known Issues - ChickenCalculator Production

## 🔴 CRITICAL: Servlet 500 Errors in Production

**Status**: UNRESOLVED - Under Active Investigation  
**Severity**: Critical  
**Impact**: All custom endpoints returning 500 errors  
**Last Updated**: January 13, 2025 01:10 PST  

### Problem Summary
All custom REST endpoints fail with HTTP 500 after controllers successfully process requests (status=200). The exception occurs in Spring MVC post-processing, not in our application code.

### Key Finding
**Controllers execute successfully** → Response shows status=200 → **Exception in Spring MVC** → 500 error returned

### What We've Ruled Out ❌
- **Write-after-commit issues** - AfterCommitGuardFilter found no violations
- **Double filter chain calls** - Fixed in JwtAuthenticationFilter and LocationAuthFilter
- **Missing converters** - Jackson present and configured correctly
- **Path pattern issues** - All Ant patterns removed, using simple string operations
- **Sentry interference** - Completely disabled
- **Controller type issues** - Using @RestController everywhere

### Diagnostic Tools in Place ✅
1. **ErrorTapFilter** - Captures ERROR dispatch (shows empty exception)
2. **ResponseProbeFilter** - Shows status=200 before exception
3. **AfterCommitGuardFilter** - Monitors post-commit writes (none found)
4. **TappingErrorAttributes** - Captures Spring errors (not being invoked)
5. **PlainErrorController** - Plain text errors (not reached)
6. **MvcDiagnostics** - Logs converters at startup
7. **FilterInventory** - Documents filter order

### Most Likely Causes (Based on Evidence)
1. **View Resolution Issue** - Controllers returning values Spring tries to resolve as views
2. **Response Type Mismatch** - Spring failing to serialize response objects
3. **Railway Environment** - Container-specific classpath or configuration issue
4. **Spring MVC Config** - WebMvcConfigurer or handler mapping problem

### Next Steps for Investigation
1. Examine controller return types and @ResponseBody usage
2. Review WebConfig.kt for MVC configuration issues  
3. Test with minimal Spring Boot defaults
4. Add diagnostic controller with explicit ResponseEntity returns

See [SERVLET_500_INVESTIGATION.md](SERVLET_500_INVESTIGATION.md) for complete investigation timeline.

## Minor Issues

### Railway Environment Variables
**Status**: Active  
**Severity**: Low  
- `FORCE_ADMIN_RESET` env var doesn't work on Railway
- Use database migrations instead for admin resets

---

## ✅ Resolved Issues

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
- Different number of HTTP message converters
- Exception stack traces not visible in production logs
- Containerized environment may affect classpath resolution

---

## Workarounds & Mitigations

### For Servlet 500 Errors (Temporary)
1. Use actuator endpoints for basic health checks (also failing currently)
2. Monitor via Railway dashboard logs
3. Use correlation IDs to track requests
4. Check ErrorTapFilter and TailLogFilter output

### For Development
1. Use dev profile with verbose logging
2. Test with `run-dev-test.bat` for consistent environment
3. Monitor MvcDiagnostics output at startup
4. Use DebugMvcController at `/debug/converters` (dev only)

---

## Contributing to Issue Resolution

If you're working on the servlet 500 error issue:

1. **Check these files first**:
   - `ErrorTapFilter.kt` - ERROR dispatch capture
   - `TailLogFilter.kt` - Request/response logging
   - `FilterInventory.kt` - Filter order verification
   - `MvcDiagnostics.kt` - Converter verification

2. **Key Railway IDs**:
   - Project: `767deec0-30ac-4238-a57b-305f5470b318`
   - Service: `fde8974b-10a3-4b70-b5f1-73c4c5cebbbe`
   - Environment: `f57580c2-24dc-4c4e-adf2-313399c855a9`

3. **Testing Commands**:
   ```bash
   # Local testing
   cd backend && mvn spring-boot:run -Dspring.profiles.active=dev
   
   # Test endpoints
   curl http://localhost:8080/minimal
   curl http://localhost:8080/test-html
   curl http://localhost:8080/api/health
   ```

4. **Production Testing**:
   ```bash
   curl https://chickencalculator-production-production-2953.up.railway.app/minimal
   ```

---

---

*Last Updated: January 13, 2025 01:10 PST*  
*Critical servlet 500 issue: Controllers work, Spring MVC post-processing fails*  
*Investigation focus: Controller return types and Spring MVC configuration*