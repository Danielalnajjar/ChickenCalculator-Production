# Testing Guide

## Current Status
- **Backend Coverage**: ~30% (tests compile but configuration broken)
- **Frontend Coverage**: ~20%
- **Target Coverage**: 80%

## Known Testing Issues

### ⚠️ CRITICAL: Test Configuration Broken
- **File**: `backend/src/test/resources/application-test.yml:3`
- **Issue**: Invalid `spring.profiles.active: test` in profile-specific resource
- **Fix**: Remove line 3, use `@ActiveProfiles("test")` annotation instead
- **Impact**: All Spring Boot tests fail to start

### AdminService Test Failing
- **File**: `AdminServiceTest.kt`
- **Issue**: Missing `@Mock PasswordEncoder` field
- **Fix**: Add mock field for password encoder
- **Impact**: 5 test methods failing

### Frontend Test Environment Missing
- **Issue**: `jest-environment-jsdom` not installed
- **Fix**: `npm install --save-dev jest-environment-jsdom`
- **Impact**: Frontend tests cannot run

## Test Commands

### Backend Testing
```bash
cd backend
mvn test                                  # Run all tests
mvn test -Dtest=ServiceTest              # Run specific test
mvn test-compile                         # Verify test compilation
```

### Frontend Testing
```bash
cd admin-portal
npm test                                  # Run tests
npm run test:coverage                    # With coverage

cd ../frontend
npm test                                  # Run tests
npm run test:coverage                    # With coverage
```

## Test Infrastructure

### Backend
- **Framework**: JUnit 5 + Mockito-Kotlin 5.1.0
- **Integration**: TestContainers 1.19.0 for PostgreSQL
- **API Testing**: RestAssured with Kotlin extensions
- **Test Base**: Comprehensive `TestBase.kt` with factory methods

### Frontend
- **Framework**: Jest 29.7.0
- **Component Testing**: React Testing Library 13.4.0
- **Coverage Thresholds**: 70% (when fixed)

## Test Organization

### Backend Test Structure
```
src/test/kotlin/com/example/chickencalculator/
├── TestBase.kt                    # Shared test utilities
├── FlywayMigrationTest.kt        # Database migration tests
├── config/
│   └── SecurityConfigTest.kt     # Security configuration
├── controller/
│   └── SpaControllerTest.kt      # Controller tests
└── service/
    ├── AdminServiceTest.kt        # Service layer tests
    ├── JwtServiceTest.kt
    └── LocationManagementServiceTest.kt
```

### Test Data Factories
The `TestBase.kt` provides factory methods for all entities:
- `createTestLocation()`
- `createTestAdminUser()`
- `createTestSalesData()`
- `createTestMarinationLog()`

## Writing New Tests

### Backend Test Template
```kotlin
@SpringBootTest
@ActiveProfiles("test")
class MyServiceTest : TestBase() {
    @Autowired
    lateinit var service: MyService
    
    @Test
    fun `should perform expected behavior`() {
        // Given
        val testData = createTestLocation()
        
        // When
        val result = service.doSomething(testData)
        
        // Then
        assertThat(result).isNotNull()
    }
}
```

### Frontend Test Template
```typescript
import { render, screen } from '@testing-library/react';
import { MyComponent } from './MyComponent';

describe('MyComponent', () => {
  it('should render correctly', () => {
    render(<MyComponent />);
    expect(screen.getByText('Expected Text')).toBeInTheDocument();
  });
});
```

## Coverage Goals

### Priority Areas for Testing
1. Authentication flows (Admin and Location)
2. Marination calculation logic
3. Multi-tenant data isolation
4. API endpoint validation
5. Frontend form validation
6. Error handling paths