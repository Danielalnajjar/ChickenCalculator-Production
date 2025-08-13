# Development Workflow

## Local Development Commands

### Backend (Spring Boot + Kotlin)
```bash
cd backend && mvn spring-boot:run                          # Run locally (H2 database)
cd backend && mvn spring-boot:run -Dspring.profiles.active=dev  # With debug tools
mvn clean package -DskipTests                             # Build JAR
mvn test                                                  # Run tests (⚠️ config broken)
mvn compile                                               # Quick compilation check
```

### Frontend (React + TypeScript)
```bash
cd admin-portal && npm start                              # Admin portal dev
cd frontend && npm start                                  # Main app dev
npm run build                                             # Production build
npm test                                                  # Run tests
```

### Docker
```bash
docker build -t chicken-calculator .                      # Build image
docker run -p 8080:8080 chicken-calculator               # Run container
```

### Deployment
```bash
git push origin main                                      # Triggers Railway auto-deploy
```

## Windows Development Scripts

Located in project root:
- `.\run-dev.bat` - Start with dev profile and environment variables
- `.\run-dev-test.bat` - Start with test configuration
- `.\test-profile-isolation.bat` - Test profile-specific components
- `.\test-local-prod.bat` - Test production profile locally

## Debug Endpoints (Dev Profile Only)
- `/probe/ok` - Basic health probe
- `/probe/boom` - Exception testing
- `/minimal` - Minimal functionality test
- `/debug/mappings` - Spring mapping inspection
- `/debug/converters` - HTTP converter debugging
- `/test` - Simple test endpoint
- `/test-html` - HTML test endpoint

## Environment Setup

### Required Environment Variables
```bash
JWT_SECRET=<32+ character secret>                        # JWT signing key
ADMIN_DEFAULT_PASSWORD=<secure-password>                 # Initial admin password
SENTRY_DSN=<sentry-dsn>                                 # Error tracking
DATABASE_URL=postgresql://...                           # Railway provides this
SPRING_PROFILES_ACTIVE=production                       # For production
```

### Optional Configuration
```bash
JWT_COOKIE_SAMESITE=Strict                             # Cookie policy
DB_POOL_SIZE=15                                        # Connection pool size
DB_MIN_IDLE=5                                          # Minimum idle connections
FILTER_LOG_LEVEL=INFO                                  # Diagnostic logging
SQL_LOG_LEVEL=INFO                                     # SQL logging
```

## Build & Test Workflow

### Pre-Deployment Checklist
1. Verify backend compilation: `mvn clean compile`
2. Verify test compilation: `mvn test-compile`
3. Build frontend: `cd admin-portal && npm run build`
4. Build main app: `cd frontend && npm run build`
5. Check Sentry for existing errors
6. Push to main branch for auto-deploy

### Post-Deployment Verification
1. Check `/api/health` endpoint
2. Monitor Sentry for new errors
3. Verify admin login works
4. Test location authentication
5. Check metrics at `/actuator/prometheus`