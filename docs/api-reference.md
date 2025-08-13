# API Reference

## Important API Notes
- **Marination Calculation**: Uses `MarinationRequest` (NOT `ChickenCalculationRequest`)
- **Service Method**: `calculateMarination()` (NOT `calculateChickenRequirements()`)
- **Response Type**: Returns `CalculationResult` (NOT `ChickenCalculationResponse`)

## Request/Response Structures

### MarinationRequest
```kotlin
data class MarinationRequest(
    val inventory: InventoryData,       // Nested object with pansSoy, pansTeriyaki, pansTurmeric
    val projectedSales: ProjectedSales, // Nested object with day0, day1, day2, day3
    val availableRawChickenKg: BigDecimal?,
    val alreadyMarinatedSoy: BigDecimal,
    val alreadyMarinatedTeriyaki: BigDecimal,
    val alreadyMarinatedTurmeric: BigDecimal
)
```

## API Endpoints

### Public Endpoints (No Auth)
```
GET  /api/health                          - System health
GET  /api/v1/calculator/locations         - Available locations
GET  /                                    - Landing page with locations
```

### Location Endpoints (Location Auth Required)
```
POST /api/v1/location/{slug}/auth/login   - Location login
POST /api/v1/location/{slug}/auth/logout  - Location logout
GET  /api/v1/location/{slug}/auth/validate - Validate session
POST /api/v1/calculator/calculate         - Marination calculation
GET  /api/v1/sales-data                   - Sales history
POST /api/v1/sales-data                   - Add sales record
GET  /api/v1/marination-log               - Marination history
POST /api/v1/marination-log               - Log marination
```

### Admin Endpoints (Admin Auth Required)
```
POST /api/v1/admin/auth/login             - Admin login
POST /api/v1/admin/auth/validate          - Token validation
POST /api/v1/admin/auth/change-password   - Change password
GET  /api/v1/admin/auth/csrf-token        - Get CSRF token
POST /api/v1/admin/auth/logout            - Logout
GET  /api/v1/admin/locations              - List locations
POST /api/v1/admin/locations              - Create location
DELETE /api/v1/admin/locations/{id}       - Delete location
PUT  /api/v1/admin/locations/{id}/password - Update location password
POST /api/v1/admin/locations/{id}/generate-password - Generate password
GET  /api/v1/admin/stats                  - Dashboard stats
```

### Monitoring Endpoints
```
GET /actuator/health                      - Detailed health
GET /actuator/prometheus                  - Prometheus metrics
GET /actuator/metrics                     - JSON metrics
```

## Authentication

### Admin Authentication
- JWT tokens in httpOnly cookies
- CSRF protection with double-submit pattern
- Password change required on first login
- Token name: `admin_token`

### Location Authentication
- Location-specific JWT tokens
- Cookie name: `location_token_{slug}`
- Rate limiting: 5 attempts, 15-minute lockout
- Session timeout: Configurable

## Response Formats

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2025-01-14T10:00:00Z"
}
```

### Error Response
```json
{
  "error": "Error message",
  "status": 400,
  "path": "/api/endpoint",
  "timestamp": "2025-01-14T10:00:00Z",
  "correlationId": "uuid"
}
```

## Business Logic Constants

### Marination Yields
- **Soy**: 16 pieces/pan, 15.6 kg yield/pan, 1 kg raw = 0.975 kg marinated
- **Teriyaki**: 13 pieces/pan, 12.675 kg yield/pan, 1 kg raw = 0.975 kg marinated
- **Turmeric**: 17 pieces/pan, 16.6 kg yield/pan, 1 kg raw = 0.976 kg marinated

### Default Portions per kg
- Soy: 9.5
- Teriyaki: 9.5
- Turmeric: 9.5