# Known Issues - ChickenCalculator Production

## Critical Issues

### 🔴 CRITICAL: Servlet 500 Errors in Production
**Status**: UNRESOLVED - Under Active Investigation  
**Severity**: Critical  
**Impact**: All endpoints returning 500 errors in production  
**Last Updated**: January 13, 2025  

#### Symptoms
- ALL endpoints return HTTP 500 Internal Server Error
- Error message: "Servlet.service() for servlet [dispatcherServlet] threw exception"
- No stack traces visible in logs
- ErrorTapFilter captures ERROR dispatch but message is empty
- Exception occurs twice per request (main request + error page processing)
- Works locally but fails on Railway production environment

#### Investigation Progress
✅ **Completed Investigations**:
- **MVC Converters**: Verified 9 converters present including MappingJackson2HttpMessageConverter
- **Jackson ObjectMapper**: Confirmed present with Kotlin module loaded
- **Auto-Configuration**: No @EnableWebMvc or WebMvcConfigurationSupport breaking Spring Boot defaults
- **Path Patterns**: Fixed all Ant-style patterns (removed **, *, {var} patterns)
- **Sentry Integration**: Disabled SENTRY_DSN as it was causing some exceptions
- **Filter Ordering**: Documented via FilterInventory
- **Dependencies**: Added jackson-datatype-jsr310 for Java time support

❌ **Still Failing Despite**:
- All compilation errors resolved
- Filters hardened with error handling
- Path normalization implemented
- Controllers using @RestController
- GlobalExceptionHandler temporarily disabled

#### Diagnostic Infrastructure Added
1. **ErrorTapFilter**: Captures ERROR dispatch at HIGHEST_PRECEDENCE
2. **TailLogFilter**: Logs request/response details for debugging
3. **FilterInventory**: Shows filter registration order at startup
4. **MvcDiagnostics**: Logs HTTP message converters at startup
5. **DebugMvcController**: Dev-only endpoint to inspect converters
6. **PathUtil**: Normalizes paths for consistent handling

#### Environment Differences
- **Local (Working)**: 10 converters, direct execution
- **Production (Failing)**: 9 converters, Railway containerized environment
- **Key Difference**: Exception details not being captured in production

#### Next Investigation Steps
1. Check for classpath/dependency conflicts in production
2. Investigate filter chain execution differences
3. Review Spring Security configuration impact
4. Check for Railway-specific environment issues
5. Deep dive into dispatcherServlet configuration

---

## Resolved Issues

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

*Last Updated: January 13, 2025 - Critical servlet issue under investigation*