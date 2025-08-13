# Known Issues - ChickenCalculator Production

*Last Updated: January 12, 2025*

## ✅ All Critical Issues Resolved!

### 1. ~~Password Change Fails After Initial Login~~ ✅ FIXED

**Status**: ✅ Resolved (December 12, 2024)  
**Severity**: ~~High~~ Fixed  
**Affects**: ~~All admin users~~ Issue resolved  
**First Reported**: December 12, 2024  
**Fixed**: December 12, 2024  
**Railway Environment**: Production

#### Description
~~Admin users cannot log in after changing their password through the admin portal. The password change endpoint returns a 401 Unauthorized error, preventing the password from being updated in the database.~~

**FIXED**: The password change feature now works correctly. Users can successfully change their passwords and log in with the new credentials.

#### Symptoms
- Admin forced to change password on first login (expected)
- Password change form submits but returns 401 error
- Admin cannot log in with either old or new password afterward
- Must use V4 migration to reset admin user

#### Root Cause Analysis (RESOLVED)

~~1. **Frontend Issue**: JWT token not properly sent with password change request~~
   - ✅ Token properly sent via httpOnly cookie with credentials: 'include'

~~2. **Backend Issue**: AdminService using private BCryptPasswordEncoder instance~~
   - ✅ **FIXED**: AdminService now uses injected PasswordEncoder bean from SecurityConfig
   - ✅ **FIXED**: CSRF exemption added for change-password endpoint

3. **Resolution Applied**:
   ```kotlin
   // AdminService.kt - NOW FIXED
   @Service
   class AdminService(
       private val adminUserRepository: AdminUserRepository,
       private val passwordEncoder: PasswordEncoder  // Now injected from SecurityConfig
   )
   ```

#### Reproduction Steps
1. Deploy fresh instance with PostgreSQL
2. Log in as admin@yourcompany.com with ADMIN_DEFAULT_PASSWORD
3. System forces password change (passwordChangeRequired=true)
4. Enter current password and new password
5. Submit form → Returns 401 Unauthorized
6. Cannot log in with any password

#### Temporary Workaround
Use V4 migration to reset admin users:
```sql
-- V4__reset_admin_password.sql
DELETE FROM admin_users;
-- AdminService will recreate on restart
```

Then set `FORCE_ADMIN_RESET=false` and restart.

#### Fix Applied (December 12, 2024)

✅ **All issues have been resolved with the following changes:**

1. **AdminService.kt**: 
   - Now uses injected PasswordEncoder bean from SecurityConfig
   - Ensures consistent password encoding across the application

2. **SecurityConfig.kt**:
   - Added `/api/v1/admin/auth/change-password` to CSRF exemptions
   - Matches pattern used for other auth endpoints

3. **AdminAuthController.kt**:
   - Added comprehensive debug logging for troubleshooting
   - Better error messages for debugging authentication issues

**Result**: Password change feature now works correctly. Users can change passwords and authenticate with new credentials.

#### Impact
- Admins locked out after mandatory password change
- Security risk if admins continue using default password
- Production deployment blocked until resolved

#### Related Files
- `backend/src/main/kotlin/com/example/chickencalculator/controller/AdminAuthController.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/service/AdminService.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/config/SecurityConfig.kt`
- `admin-portal/src/components/PasswordChangeModal.tsx`
- `backend/src/main/resources/db/migration/V4__reset_admin_password.sql`

---

## Current Status

### ✅ Multi-Location Authentication (January 12, 2025)
- **Status**: Successfully deployed and operational
- **Features**: 
  - Password-protected location access
  - Rate limiting (5 attempts, 15-minute lockout)
  - Session isolation between locations
  - Location-specific JWT tokens
- **Migration**: V5 applied successfully

## Minor Issues

### 1. FORCE_ADMIN_RESET Environment Variable Not Working

**Status**: 🟡 Active  
**Severity**: Low (V4 migration provides alternative)

#### Description
Setting `FORCE_ADMIN_RESET=true` in Railway doesn't trigger admin deletion. The value appears as 'false' in application logs even when set to 'true' in Railway dashboard.

#### Workaround
Use V4 migration instead of environment variable.

---

## Resolved Issues

### 2. ~~H2 to PostgreSQL Migration~~ ✅

**Status**: Resolved (December 12, 2024)  
**Resolution**: Successfully migrated to PostgreSQL on Railway

Key fixes applied:
- Fixed "pool is sealed" error in DatabaseConfig
- Fixed Flyway execution order
- Added PostgreSQL sequence support
- Created V4 migration for admin reset

### 3. ~~Compilation Errors~~ ✅

**Status**: Resolved (December 2024)  
**Resolution**: Fixed all 26 compilation errors

Major fixes:
- Micrometer API compatibility
- Sentry 7.0.0 updates
- Test entity constructors
- Repository methods

---

## Monitoring

### Error Tracking
Monitor these log patterns for password change issues:
```
"Password change attempt with invalid current password"
"Authentication attempt for email"
"Password verification failed"
"JWT token validation failed"
```

### Metrics to Watch
- `chicken.calculator.admin.change_password.time`
- `chicken.calculator.admin.login.time`
- Auth failure rate by type

---

## Prevention Measures

1. **Testing Requirements**:
   - Add integration test for full password change flow
   - Test with httpOnly cookies enabled
   - Verify JWT token transmission

2. **Code Review Focus**:
   - Authentication endpoints
   - Password encoder configuration
   - CORS and cookie settings

3. **Deployment Checklist Addition**:
   - Test password change after each deployment
   - Verify JWT cookie transmission
   - Check CORS configuration

---

*Status as of January 12, 2025: System fully operational with multi-location authentication*