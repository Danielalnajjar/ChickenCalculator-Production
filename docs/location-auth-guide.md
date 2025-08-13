# Location Authentication Guide

*Created: January 12, 2025*

## Overview

The ChickenCalculator now implements a comprehensive multi-location authentication system where each restaurant location has its own password-protected access. This ensures complete data isolation and security for multi-tenant operations.

## Key Features

### 🔒 Security Features
- **Per-Location Passwords**: Each location has its own password
- **BCrypt Hashing**: Secure password storage with 10 rounds
- **Rate Limiting**: 5 failed attempts triggers 15-minute lockout
- **Session Management**: 8-hour default timeout (configurable)
- **JWT Tokens**: Location-specific tokens in httpOnly cookies
- **Session Isolation**: No data leakage between locations

### 🎯 User Experience
- **Beautiful Login Page**: Gradient design with brand consistency
- **Landing Page**: Shows available locations
- **Persistent Sessions**: Stay logged in for 8 hours
- **Automatic Redirect**: Return to requested page after login
- **Mobile Responsive**: Works on all devices

## Admin Guide

### Creating a Location

1. **Login to Admin Portal**
   ```
   URL: https://your-app.railway.app/admin
   Credentials: admin@yourcompany.com
   ```

2. **Create New Location**
   - Navigate to Locations section
   - Click "Add Location"
   - Fill in location details:
     - Name (e.g., "Downtown Branch")
     - Manager Name
     - Manager Email
     - Address (optional)

3. **Set Location Password**
   - Default password: `ChangeMe123!`
   - Options:
     - Keep default (not recommended)
     - Set custom password
     - Generate secure password

### Managing Location Passwords

#### Option 1: Set Custom Password
```http
PUT /api/v1/admin/locations/{id}/password
{
  "password": "YourSecurePassword123!"
}
```

#### Option 2: Generate Secure Password
```http
POST /api/v1/admin/locations/{id}/generate-password
```
Response includes the generated password - save it immediately!

### Monitoring Location Access

- View failed login attempts in location details
- Check last password change date
- Monitor session timeout settings
- Track authentication metrics in Prometheus

## Location User Guide

### Accessing Your Location

1. **Navigate to Location URL**
   ```
   https://your-app.railway.app/{location-slug}
   ```
   Example: `https://your-app.railway.app/downtown-branch`

2. **Login with Location Password**
   - Enter the password provided by your administrator
   - Password is case-sensitive
   - After 5 failed attempts, wait 15 minutes

3. **Access Location Features**
   - Calculator: `/{slug}/calculator`
   - Sales Data: `/{slug}/sales`
   - History: `/{slug}/history`

### Session Management

- **Session Duration**: 8 hours by default
- **Cookie Name**: `location_token_{slug}`
- **Logout**: Available in navigation menu
- **Auto-Logout**: After session timeout

## Technical Implementation

### Backend Components

#### LocationAuthService
```kotlin
@Service
class LocationAuthService(
    private val locationRepository: LocationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    fun authenticateLocation(slug: String, password: String): String
    fun validateLocationToken(token: String, slug: String): Boolean
    fun changeLocationPassword(locationId: Long, newPassword: String): Location
}
```

#### LocationAuthController
```kotlin
@RestController
@RequestMapping("/api/v1/location/{slug}/auth")
class LocationAuthController {
    @PostMapping("/login")
    fun login(@PathVariable slug: String, @RequestBody request: LocationAuthRequest)
    
    @PostMapping("/logout")
    fun logout(@PathVariable slug: String)
    
    @GetMapping("/validate")
    fun validateSession(@PathVariable slug: String)
}
```

#### LocationAuthFilter
```kotlin
@Component
class LocationAuthFilter : OncePerRequestFilter() {
    // Validates JWT tokens for location-specific requests
    // Extracts location context from token
    // Sets location ID in request attributes
}
```

### Frontend Components

#### LocationContext
```typescript
interface LocationContextType {
  location: Location | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (slug: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
}
```

#### LocationLogin Component
```typescript
const LocationLogin: React.FC = () => {
  // Beautiful gradient login page
  // Rate limiting feedback
  // Error handling
}
```

#### RequireAuth Component
```typescript
const RequireAuth: React.FC = ({ children }) => {
  // Protects routes requiring authentication
  // Redirects to login if not authenticated
  // Preserves requested location
}
```

### Database Schema

#### Location Table (V5 Migration)
```sql
ALTER TABLE locations ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE locations ADD COLUMN requires_auth BOOLEAN DEFAULT TRUE;
ALTER TABLE locations ADD COLUMN session_timeout_hours INTEGER DEFAULT 8;
ALTER TABLE locations ADD COLUMN failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE locations ADD COLUMN last_failed_login TIMESTAMP;
ALTER TABLE locations ADD COLUMN last_password_change TIMESTAMP;
```

## Security Considerations

### Password Requirements
- Minimum 8 characters recommended
- Should include uppercase, lowercase, numbers
- Avoid common patterns
- Change default password immediately

### Rate Limiting
- 5 failed attempts allowed
- 15-minute lockout period
- Lockout resets after successful login
- Admin can reset via database if needed

### Session Security
- JWT tokens in httpOnly cookies
- CSRF protection enabled
- Secure flag on cookies (HTTPS only)
- SameSite=Strict for cookies

### Data Isolation
- No default location fallback
- Location context required for all operations
- Service layer enforces boundaries
- Repository queries scoped to location

## Troubleshooting

### Common Issues

#### "Invalid credentials" Error
- Verify password is correct
- Check for caps lock
- Ensure not in lockout period
- Contact admin for password reset

#### Session Expired
- Sessions last 8 hours by default
- Login again to continue
- Admin can adjust timeout if needed

#### Cannot Access Location
- Verify location slug is correct
- Check location status is ACTIVE
- Ensure location requires_auth is true
- Verify password has been set

### Admin Recovery

#### Reset Location Password
```sql
-- Connect to PostgreSQL
UPDATE locations 
SET password_hash = '$2a$10$ZH7XRVxRNWxsU.YLNxKfCOPwLGX4MKfGl9oJNB8Kg1.pVHVqg/h2e',
    failed_login_attempts = 0,
    last_failed_login = NULL
WHERE slug = 'location-slug';
-- Password is now: ChangeMe123!
```

#### Disable Authentication (Emergency)
```sql
UPDATE locations 
SET requires_auth = FALSE 
WHERE slug = 'location-slug';
-- Location now accessible without password
```

## Monitoring

### Metrics to Track
- `location.auth.attempts` - Login attempts by location
- `location.auth.failures` - Failed logins by location
- `location.auth.lockouts` - Lockout events
- `location.sessions.active` - Active sessions
- `location.sessions.timeout` - Session timeouts

### Logs to Monitor
```
"Location authentication attempt for slug: {}"
"Location authentication successful for slug: {}"
"Location authentication failed for slug: {}"
"Location locked out due to failed attempts: {}"
"Location password changed for ID: {}"
```

## Future Enhancements

### Planned Features
- [ ] Two-factor authentication
- [ ] Password reset via email
- [ ] Session activity monitoring
- [ ] IP-based access control
- [ ] Password expiration policies
- [ ] Audit trail for access

### Considered Improvements
- OAuth2/SAML integration
- Biometric authentication
- Hardware token support
- Advanced rate limiting rules
- Geolocation restrictions

## Migration from Previous Version

### For Existing Deployments

1. **Apply V5 Migration**
   - Automatically adds auth fields
   - Sets default password for all locations
   - Creates necessary indexes

2. **Update Location Passwords**
   - Login to admin portal
   - Generate new passwords for each location
   - Distribute to location managers

3. **Notify Users**
   - Inform about new authentication requirement
   - Provide location-specific URLs
   - Share password securely

4. **Monitor Adoption**
   - Track login success rates
   - Address authentication issues
   - Adjust session timeouts as needed

## Support

### For Administrators
- Review this guide thoroughly
- Test authentication flow
- Monitor authentication metrics
- Keep passwords secure

### For Location Users
- Contact your administrator for passwords
- Report authentication issues immediately
- Keep your password confidential
- Logout when finished

---

*This guide covers the multi-location authentication system implemented in January 2025. For general system documentation, see CLAUDE.md.*