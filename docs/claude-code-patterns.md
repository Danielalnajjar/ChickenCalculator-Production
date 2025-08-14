# Claude Code Patterns

## Common Workflows for ChickenCalculator

### 1. Debug Production Error
```bash
# Start with Sentry
mcp__sentry__search_issues(organizationSlug: "wok-to-walk", naturalLanguageQuery: "unresolved errors last hour")
mcp__sentry__analyze_issue_with_seer(organizationSlug: "wok-to-walk", issueId: "[ISSUE-ID]")

# Check deployment status
mcp__railway__deployment_list(projectId: "767deec0-30ac-4238-a57b-305f5470b318", serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")

# View recent logs
mcp__railway__deployment_logs(deploymentId: "[from list]")
```

### 2. Deploy New Feature
```bash
# Pre-deployment checks
mvn clean compile
mvn test-compile
cd admin-portal && npm run build
cd ../frontend && npm run build

# Check for existing errors
mcp__sentry__search_events(organizationSlug: "wok-to-walk", naturalLanguageQuery: "error count last hour")

# Deploy
git add .
git commit -m "feat: description"
git push origin main

# Monitor deployment
mcp__railway__deployment_status(deploymentId: "[latest]")
```

### 3. Fix Test Issues
```bash
# Remove broken test config
Edit backend/src/test/resources/application-test.yml
# Remove line 3: spring.profiles.active: test

# Fix missing test dependencies
cd admin-portal && npm install --save-dev jest-environment-jsdom
cd ../frontend && npm install --save-dev jest-environment-jsdom

# Run tests
cd backend && mvn test
cd ../admin-portal && npm test
```

### 4. Update Environment Variables
```bash
# View current variables
mcp__railway__list_service_variables(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")

# Update variable
mcp__railway__variable_set(projectId: "767deec0-30ac-4238-a57b-305f5470b318", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9", name: "VAR_NAME", value: "value")

# Restart service
mcp__railway__service_restart(serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
```

### 5. Local Development Setup
```bash
# Backend with dev profile
cd backend && mvn spring-boot:run -Dspring.profiles.active=dev

# Or use Windows script
.\run-dev.bat

# Frontend development
cd frontend && npm start

# Admin portal
cd admin-portal && npm start
```

### 6. Database Migration
```bash
# Create migration file
Write backend/src/main/resources/db/migration/V6__description.sql

# Test locally first
mvn spring-boot:run -Dspring.profiles.active=dev

# Deploy (Flyway runs automatically)
git push origin main
```

### 7. Quick Health Check
```bash
# Check production health
curl https://chickencalculator-production-production-2953.up.railway.app/api/health

# Check Sentry for errors
mcp__sentry__search_events(organizationSlug: "wok-to-walk", naturalLanguageQuery: "error count last 15 minutes")

# Check metrics
curl https://chickencalculator-production-production-2953.up.railway.app/actuator/prometheus
```

### 8. Create New Location
```bash
# Via API (as admin)
POST /api/v1/admin/locations
{
  "name": "Location Name",
  "slug": "location-slug"
}

# Generate password
POST /api/v1/admin/locations/{id}/generate-password
```

### 9. Monitor After Deployment
```bash
# Check deployment completed
mcp__railway__deployment_status(deploymentId: "[latest]")

# Monitor for new errors (wait 2-3 minutes)
mcp__sentry__search_issues(organizationSlug: "wok-to-walk", naturalLanguageQuery: "new errors last 5 minutes")

# Verify endpoints
curl https://chickencalculator-production-production-2953.up.railway.app/api/health
```

### 10. Rollback If Needed
```bash
# Via Railway Dashboard
mcp__railway__deployment_list(projectId: "767deec0-30ac-4238-a57b-305f5470b318", serviceId: "fde8974b-10a3-4b70-b5f1-73c4c5cebbbe", environmentId: "f57580c2-24dc-4c4e-adf2-313399c855a9")
# Find previous successful deployment
# Redeploy via Railway dashboard

# Or via Git
git revert HEAD
git push origin main
```

## Code Implementation Patterns

### Service Layer Pattern
```kotlin
@Service
@Transactional
class LocationService(
    private val locationRepository: LocationRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun createLocation(request: CreateLocationRequest): LocationResponse {
        // Validate input
        validateLocationRequest(request)
        
        // Business logic
        val location = Location(
            name = request.name,
            slug = request.slug.lowercase(),
            passwordHash = passwordEncoder.encode(request.password)
        )
        
        // Persist
        val saved = locationRepository.save(location)
        
        // Return DTO
        return LocationResponse.from(saved)
    }
}
```

### Controller Pattern
```kotlin
@RestController
@RequestMapping("/api/v1/locations")
class LocationController(
    private val locationService: LocationService
) {
    @PostMapping
    fun createLocation(
        @Valid @RequestBody request: CreateLocationRequest
    ): ResponseEntity<LocationResponse> {
        val response = locationService.createLocation(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                error = ex.message,
                correlationId = MDC.get("correlationId")
            )
        )
    }
}
```

### Repository Pattern with Custom Query
```kotlin
interface LocationRepository : JpaRepository<Location, Long> {
    @Query("SELECT l FROM Location l WHERE l.slug = :slug AND l.deletedAt IS NULL")
    fun findActiveBySlug(@Param("slug") slug: String): Optional<Location>
    
    @Modifying
    @Query("UPDATE Location l SET l.lastAccessed = :timestamp WHERE l.id = :id")
    fun updateLastAccessed(@Param("id") id: Long, @Param("timestamp") timestamp: Instant)
}
```

### React Component Pattern
```typescript
interface CalculatorProps {
    locationId: string;
    onCalculate: (result: CalculationResult) => void;
}

const Calculator: React.FC<CalculatorProps> = ({ locationId, onCalculate }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    
    const handleSubmit = async (data: CalculationRequest) => {
        setLoading(true);
        setError(null);
        
        try {
            const result = await api.calculate(locationId, data);
            onCalculate(result);
        } catch (err) {
            setError(err.message || 'Calculation failed');
        } finally {
            setLoading(false);
        }
    };
    
    return (
        <form onSubmit={handleSubmit}>
            {error && <Alert severity="error">{error}</Alert>}
            {/* form fields */}
        </form>
    );
};
```

### Error Handling Pattern
```kotlin
@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("Entity not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                error = ex.message ?: "Resource not found",
                status = 404,
                correlationId = MDC.get("correlationId")
            )
        )
    }
    
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConstraintViolation(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        logger.error("Database constraint violation", ex)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                error = "Data conflict - please check your input",
                status = 409,
                correlationId = MDC.get("correlationId")
            )
        )
    }
}
```

### Test Pattern
```kotlin
@SpringBootTest
@ActiveProfiles("test")
class LocationServiceTest : TestBase() {
    @Autowired
    lateinit var locationService: LocationService
    
    @Test
    fun `should create location with valid data`() {
        // Given
        val request = CreateLocationRequest(
            name = "Test Location",
            slug = "test-location",
            password = "SecurePass123!"
        )
        
        // When
        val response = locationService.createLocation(request)
        
        // Then
        assertThat(response).isNotNull()
        assertThat(response.slug).isEqualTo("test-location")
        assertThat(response.name).isEqualTo("Test Location")
    }
}
```

## Best Practices

### Always Start with Sentry
Before investigating any issue, check Sentry for existing errors and patterns.

### Use TodoWrite for Complex Tasks
Break down multi-step operations into todos for better tracking.

### Batch MCP Commands
Run multiple MCP commands in parallel when gathering information.

### Verify Before Deployment
Always run compilation and build checks before pushing to main.

### Monitor After Changes
Wait 2-3 minutes after deployment, then check Sentry for new errors.

### Follow Existing Patterns
Look at neighboring files and existing implementations before writing new code.

### Use Constructor Injection
Prefer constructor injection over field injection for better testability.

### Return DTOs, Not Entities
Never return JPA entities directly from controllers; always use DTOs.

### Log Before Throwing
Always log errors with context before throwing exceptions.