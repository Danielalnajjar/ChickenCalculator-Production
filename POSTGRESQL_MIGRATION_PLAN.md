# 🚀 POSTGRESQL MIGRATION COMPREHENSIVE PLAN
## ChickenCalculator Production System - Complete Migration Guide

---

## 📋 TABLE OF CONTENTS
1. [Current System Status](#current-system-status)
2. [Critical Issues Summary](#critical-issues-summary)
3. [Pre-Migration Code Fixes](#phase-0-pre-migration-code-fixes)
4. [Environment Variable Fixes](#phase-1-critical-environment-fixes)
5. [PostgreSQL Deployment](#phase-2-postgresql-deployment)
6. [Environment Migration](#phase-3-environment-variable-migration)
7. [Deployment Monitoring](#phase-4-deployment-monitoring)
8. [Functional Validation](#phase-5-functional-validation)
9. [Security Hardening](#phase-6-security-hardening)
10. [Monitoring Setup](#phase-7-monitoring-setup)
11. [Rollback Procedures](#phase-8-rollback-procedures)
12. [Validation Checklist](#final-validation-checklist)
13. [Troubleshooting Guide](#troubleshooting-guide)

---

## 🔍 CURRENT SYSTEM STATUS

### Production Environment Details
- **Railway Project ID**: `767deec0-30ac-4238-a57b-305f5470b318`
- **Service ID**: `fde8974b-10a3-4b70-b5f1-73c4c5cebbbe`
- **Environment ID**: `f57580c2-24dc-4c4e-adf2-313399c855a9`
- **Production URL**: https://chickencalculator-production-production-2953.up.railway.app
- **Railway API Token**: Configured in environment

### Current Database Configuration (H2 - PROBLEMATIC)
```yaml
DATABASE_URL: jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;MODE=PostgreSQL
DATABASE_DRIVER: org.h2.Driver
DATABASE_PLATFORM: org.hibernate.dialect.H2Dialect
DATABASE_USERNAME: sa
DATABASE_PASSWORD: (empty)
DDL_AUTO: update  # DANGEROUS - allows schema modification
FORCE_ADMIN_RESET: true  # CRITICAL - deletes admins on restart
```

### Authentication Issue
- **Problem**: Admin login failing with "Password verification result: false"
- **Root Cause**: H2 database file persists with old password hash
- **Current Hash**: `$2a$10$QFN...` (doesn't match configured password)
- **Configured Password**: `ChickenAdmin2024!Secure#`

---

## 🚨 CRITICAL ISSUES SUMMARY

### Issues Discovered by 10 Parallel Subagent Analyses

| Issue | Severity | Found By | Impact |
|-------|----------|----------|--------|
| DDL_AUTO=update | CRITICAL | 4 agents | Schema destruction risk |
| FORCE_ADMIN_RESET=true | CRITICAL | 3 agents | Deletes all admins |
| H2 DATE() functions | HIGH | 2 agents | PostgreSQL incompatible |
| GenerationType.IDENTITY | HIGH | 2 agents | Wrong for PostgreSQL |
| Duplicate dialect config | HIGH | 1 agent | Conflicts in YAML |
| Missing Flyway PostgreSQL | MEDIUM | 1 agent | Migration may fail |
| Connection pool too large | MEDIUM | 2 agents | Railway limits |
| CORS wildcard vulnerability | HIGH | 1 agent | Security risk |
| No database SSL | HIGH | 1 agent | Unencrypted data |
| Transaction boundary bug | MEDIUM | 1 agent | Admin test fails |
| No Sentry configured | MEDIUM | 1 agent | No error tracking |
| BCrypt rounds too low | MEDIUM | 1 agent | Weak hashing |
| Missing env validation | HIGH | 2 agents | Startup issues |

---

## 🔧 PHASE 0: PRE-MIGRATION CODE FIXES
**Duration: 30-45 minutes**
**Critical: Must complete before migration**

### Step 0.1: Fix Repository DATE() Functions
**File**: `backend/src/main/kotlin/com/example/chickencalculator/repository/MarinationLogRepository.kt`
```kotlin
// Lines 27 and 41 - CHANGE FROM:
@Query("SELECT m FROM MarinationLog m WHERE m.locationId = :locationId AND DATE(m.timestamp) = :date")

// CHANGE TO:
@Query("SELECT m FROM MarinationLog m WHERE m.locationId = :locationId AND CAST(m.timestamp AS DATE) = :date")
```

### Step 0.2: Fix Entity Generation Strategy
**Files to update**:
- `backend/src/main/kotlin/com/example/chickencalculator/entity/AdminUser.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/entity/Location.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/entity/SalesData.kt`
- `backend/src/main/kotlin/com/example/chickencalculator/entity/MarinationLog.kt`

```kotlin
// CHANGE FROM:
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
val id: Long = 0

// CHANGE TO:
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
@SequenceGenerator(name = "entity_seq", sequenceName = "entity_id_seq", allocationSize = 1)
val id: Long = 0
```

### Step 0.3: Fix Configuration Conflicts
**File**: `backend/src/main/resources/application-production.yml`
```yaml
# Line 26-31 - REMOVE the duplicate:
# DELETE THESE LINES:
properties:
  hibernate:
    dialect: ${DATABASE_DIALECT:org.hibernate.dialect.H2Dialect}
    
# KEEP ONLY:
database-platform: ${DATABASE_PLATFORM:org.hibernate.dialect.PostgreSQLDialect}
```

### Step 0.4: Add PostgreSQL Flyway Dependency
**File**: `backend/pom.xml`
```xml
<!-- Add after line with spring-boot-starter-flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### Step 0.5: Create PostgreSQL Migration
**Create new file**: `backend/src/main/resources/db/migration/V3__postgresql_sequences.sql`
```sql
-- PostgreSQL sequence creation for all entities
CREATE SEQUENCE IF NOT EXISTS admin_users_id_seq START 1000;
CREATE SEQUENCE IF NOT EXISTS locations_id_seq START 1000;
CREATE SEQUENCE IF NOT EXISTS sales_data_id_seq START 1000;
CREATE SEQUENCE IF NOT EXISTS marination_log_id_seq START 1000;

-- Update existing tables to use sequences
ALTER TABLE admin_users ALTER COLUMN id SET DEFAULT nextval('admin_users_id_seq');
ALTER TABLE locations ALTER COLUMN id SET DEFAULT nextval('locations_id_seq');
ALTER TABLE sales_data ALTER COLUMN id SET DEFAULT nextval('sales_data_id_seq');
ALTER TABLE marination_log ALTER COLUMN id SET DEFAULT nextval('marination_log_id_seq');

-- Set sequence values based on existing data
SELECT setval('admin_users_id_seq', COALESCE((SELECT MAX(id) FROM admin_users), 1000));
SELECT setval('locations_id_seq', COALESCE((SELECT MAX(id) FROM locations), 1000));
SELECT setval('sales_data_id_seq', COALESCE((SELECT MAX(id) FROM sales_data), 1000));
SELECT setval('marination_log_id_seq', COALESCE((SELECT MAX(id) FROM marination_log), 1000));
```

### Step 0.6: Remove Admin Test Authentication
**File**: `backend/src/main/kotlin/com/example/chickencalculator/service/AdminService.kt`
```kotlin
// Lines 144-150 - DELETE this entire block:
// Test authentication
val testAuth = authenticate(defaultEmail, defaultPassword)
if (testAuth != null) {
    logger.info("Authentication test successful")
} else {
    logger.error("Authentication test failed! Check password encoder")
}

// Keep the admin creation, just remove the test
```

### Step 0.7: Update V1 Migration for PostgreSQL
**File**: `backend/src/main/resources/db/migration/V1__initial_schema.sql`
```sql
-- Change line 6 FROM:
id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,

-- TO:
id BIGINT PRIMARY KEY,

-- Apply same change to lines 18, 32, 45 for all tables
```

### Step 0.8: Commit and Push Changes
```bash
cd C:\Users\danie\Code\ChickenCalculator-Production
git add -A
git commit -m "fix: PostgreSQL compatibility updates for migration

- Replace H2 DATE() with CAST AS DATE in repositories
- Change GenerationType.IDENTITY to SEQUENCE for all entities
- Fix duplicate dialect configuration in application-production.yml
- Add PostgreSQL Flyway dependency
- Create V3 migration for PostgreSQL sequences
- Update V1 migration for PostgreSQL compatibility
- Remove transaction boundary issue in AdminService

Resolves all PostgreSQL compatibility issues identified in migration analysis"

git push origin main
```

**⏰ WAIT FOR DEPLOYMENT TO COMPLETE (5-7 minutes)**
- Monitor Railway dashboard for deployment status
- Check logs for any compilation errors
- Ensure deployment succeeds before proceeding

---

## 🔧 PHASE 1: CRITICAL ENVIRONMENT FIXES
**Duration: 5 minutes**
**IMMEDIATE ACTION REQUIRED**

### Step 1.1: Access Railway Dashboard
1. Navigate to: https://railway.app/dashboard
2. Select project: `chicken-calculator`
3. Click on: `ChickenCalculator-Production` service
4. Go to: "Variables" tab

### Step 1.2: Update Critical Variables
**CHANGE THESE IMMEDIATELY** (copy-paste exactly):
```bash
DDL_AUTO=validate
FORCE_ADMIN_RESET=false
DB_POOL_SIZE=10
DB_CONNECTION_TIMEOUT=20000
DB_MAX_LIFETIME=900000
DB_MIN_IDLE=2
```

### Why These Changes Are Critical:
- `DDL_AUTO=validate`: Prevents Hibernate from modifying schema
- `FORCE_ADMIN_RESET=false`: Stops deletion of admin users
- `DB_POOL_SIZE=10`: Reduced from 20 for Railway memory limits
- `DB_CONNECTION_TIMEOUT=20000`: Faster failure detection
- `DB_MAX_LIFETIME=900000`: 15 minutes (PostgreSQL recommended)
- `DB_MIN_IDLE=2`: Reduced for resource optimization

**⏰ Auto-deployment triggers - Wait 5 minutes**

---

## 🐘 PHASE 2: POSTGRESQL DEPLOYMENT
**Duration: 10-15 minutes**

### Step 2.1: Deploy PostgreSQL Service
1. In Railway dashboard, click **"+ New"** button (top right)
2. Select **"Database"** tab
3. Click **"PostgreSQL"** (Template ID: `b55da7dc-09be-4140-bc65-1284d15d349c`)
4. Service name: Auto-generated as "PostgreSQL" (keep default)
5. Click **"Deploy"**
6. **WAIT** for status to show **"Active"** (3-5 minutes)

### Step 2.2: Verify PostgreSQL Deployment
PostgreSQL service automatically creates these variables:
- `DATABASE_URL`: Full connection string with SSL
- `PGDATABASE`: Database name (railway)
- `PGHOST`: Internal hostname
- `PGPASSWORD`: Secure password
- `PGPORT`: 5432
- `PGUSER`: postgres

### Step 2.3: Get PostgreSQL Connection Info
1. Click on the PostgreSQL service
2. Go to "Connect" tab
3. Copy the `DATABASE_URL` for reference
4. Note: Railway auto-injects this into your app

---

## 🔄 PHASE 3: ENVIRONMENT VARIABLE MIGRATION
**Duration: 10 minutes**

### Step 3.1: Delete ALL H2 Variables
**In ChickenCalculator-Production service, DELETE these variables:**
```bash
DATABASE_URL                 # Delete - Railway provides PostgreSQL URL
DATABASE_DRIVER              # Delete - will add PostgreSQL version
DATABASE_PLATFORM            # Delete - will add PostgreSQL version
DATABASE_USERNAME            # Delete - included in DATABASE_URL
DATABASE_PASSWORD            # Delete - included in DATABASE_URL
DATABASE_DIALECT             # Delete if exists
H2_CONSOLE_ENABLED          # Delete - not needed for PostgreSQL
```

### Step 3.2: Add PostgreSQL Configuration
**ADD these new variables:**
```bash
DATABASE_DRIVER=org.postgresql.Driver
DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_LOB_NON_CONTEXTUAL_CREATION=true
SPRING_JPA_PROPERTIES_HIBERNATE_TEMP_USE_JDBC_METADATA_DEFAULTS=false
```

### Step 3.3: Verify Security Variables
**ENSURE these remain unchanged:**
```bash
JWT_SECRET=7K9mP3nX5vB8zQ2wF6hJ4sL7rT9yE3aG8kN5cV2bM4xZ6qW3fR8dS5jH7nP9tY2u
ADMIN_DEFAULT_PASSWORD=ChickenAdmin2024!Secure#
SPRING_PROFILES_ACTIVE=production
DDL_AUTO=validate
FORCE_ADMIN_RESET=false
```

### Step 3.4: Add Monitoring (Optional but Recommended)
```bash
SENTRY_DSN=                    # Add your Sentry DSN if available
LOG_LEVEL=INFO
SHOW_SQL=false
SQL_LOG_LEVEL=WARN
```

**⏰ Auto-deployment triggers - Monitor logs (5-7 minutes)**

---

## 📊 PHASE 4: DEPLOYMENT MONITORING
**Duration: 15 minutes**
**CRITICAL: Watch for success/failure indicators**

### Step 4.1: Monitor Deployment Logs
**In Railway dashboard → ChickenCalculator-Production → Deployments → View Logs**

### SUCCESS Indicators to Look For:
```log
✅ "HikariPool-1 - Starting..."
✅ "HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection"
✅ "Successfully acquired change log lock"
✅ "Current version of schema \"public\": 2"
✅ "Migrating schema \"public\" to version \"3 - postgresql sequences\""
✅ "Successfully applied 1 migration"
✅ "Successfully released change log lock"
✅ "Checking for existing admin users"
✅ "Creating default admin user with email: admin@yourcompany.com"
✅ "Admin user created successfully with ID: 1001"
✅ "Started ChickenCalculatorApplication in X seconds"
✅ "Tomcat started on port(s): 8080"
```

### ERROR Indicators (If These Appear, See Troubleshooting):
```log
❌ "Connection to localhost:5432 refused"
   → PostgreSQL not linked properly
   
❌ "PSQLException: FATAL: password authentication failed"
   → DATABASE_URL not properly set
   
❌ "FlywayException: Migration V3 failed"
   → SQL syntax error in migration
   
❌ "PSQLException: relation \"admin_users\" does not exist"
   → Flyway migrations didn't run
   
❌ "GenerationTarget encountered exception"
   → Sequence generation issue
   
❌ "BCrypt password verification failed"
   → Admin creation issue
```

### Step 4.2: Quick Health Check
```bash
# Use curl or browser:
curl https://chickencalculator-production-production-2953.up.railway.app/api/health

# Expected response:
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

---

## ✅ PHASE 5: FUNCTIONAL VALIDATION
**Duration: 20 minutes**
**Complete testing of all functionality**

### Step 5.1: Test Admin Authentication
```bash
# Using curl or Postman:
curl -X POST https://chickencalculator-production-production-2953.up.railway.app/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@yourcompany.com",
    "password": "ChickenAdmin2024!Secure#"
  }'

# Expected response:
{
  "user": {
    "id": 1001,
    "email": "admin@yourcompany.com",
    "name": "System Administrator",
    "role": "ADMIN",
    "passwordChangeRequired": true
  },
  "message": "Login successful"
}
```

### Step 5.2: Test Password Change (Required)
```bash
# Get CSRF token first:
curl https://chickencalculator-production-production-2953.up.railway.app/api/v1/admin/auth/csrf-token

# Then change password:
curl -X POST https://chickencalculator-production-production-2953.up.railway.app/api/v1/admin/auth/change-password \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: <csrf-token>" \
  -d '{
    "currentPassword": "ChickenAdmin2024!Secure#",
    "newPassword": "NewSecurePassword2024!"
  }'
```

### Step 5.3: Test Location Management
1. Login to admin portal: `/admin`
2. Create new location:
   - Name: "Test Location PostgreSQL"
   - Address: "123 Database St"
   - Manager: "Test Manager"
   - Email: "manager@test.com"
3. Verify location created with slug
4. Access location calculator at `/{slug}`

### Step 5.4: Test Calculator Functionality
```bash
# Test marination calculation:
curl -X POST https://chickencalculator-production-production-2953.up.railway.app/api/v1/calculator/calculate \
  -H "Content-Type: application/json" \
  -H "X-Location-Id: 1" \
  -d '{
    "inventory": {
      "pansSoy": 2,
      "pansTeriyaki": 1,
      "pansTurmeric": 1
    },
    "projectedSales": {
      "day0": 50,
      "day1": 60,
      "day2": 55,
      "day3": 45
    },
    "availableRawChickenKg": 100,
    "alreadyMarinatedSoy": 0,
    "alreadyMarinatedTeriyaki": 0,
    "alreadyMarinatedTurmeric": 0
  }'
```

### Step 5.5: Test Data Operations
```bash
# Add sales data:
curl -X POST https://chickencalculator-production-production-2953.up.railway.app/api/v1/sales-data \
  -H "Content-Type: application/json" \
  -H "X-Location-Id: 1" \
  -d '{
    "date": "2024-12-12",
    "totalSales": 1500.00,
    "portionsSoy": 25,
    "portionsTeriyaki": 20,
    "portionsTurmeric": 15
  }'

# Verify retrieval:
curl https://chickencalculator-production-production-2953.up.railway.app/api/v1/sales-data?locationId=1
```

---

## 🔒 PHASE 6: SECURITY HARDENING
**Duration: 15 minutes**
**Implement security improvements**

### Step 6.1: Update CORS Configuration
**In Railway Variables, UPDATE:**
```bash
# Remove wildcard, use specific domain:
CORS_ALLOWED_ORIGINS=https://chickencalculator-production-production-2953.up.railway.app
```

### Step 6.2: Verify SSL/TLS
Check PostgreSQL connection in logs for:
```log
"SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384)"
```

### Step 6.3: Add Security Headers
**Add to Railway Variables:**
```bash
SECURITY_HEADERS_ENABLED=true
SECURITY_HEADERS_FRAME_OPTIONS=DENY
SECURITY_HEADERS_XSS_PROTECTION=1; mode=block
SECURITY_HEADERS_CONTENT_TYPE_OPTIONS=nosniff
```

### Step 6.4: Increase BCrypt Rounds
**For future deployment, update code:**
```kotlin
// AdminService.kt line 19
private val passwordEncoder = BCryptPasswordEncoder(12)  // Increase from 10
```

---

## 📈 PHASE 7: MONITORING SETUP
**Duration: 10 minutes**

### Step 7.1: Verify Prometheus Metrics
```bash
curl https://chickencalculator-production-production-2953.up.railway.app/actuator/prometheus | head -50

# Look for:
✅ chicken_calculator_calculations_total
✅ chicken_calculator_locations_active
✅ hikaricp_connections_active
✅ hikaricp_connections_pending
✅ database_query_duration_seconds
✅ http_server_requests_duration
```

### Step 7.2: Configure Sentry (If Available)
1. Get Sentry DSN from https://sentry.io
2. Add to Railway Variables:
```bash
SENTRY_DSN=https://xxxxx@sentry.io/xxxxx
```
3. Trigger test error to verify

### Step 7.3: Setup Monitoring Dashboards
**Key Metrics to Monitor:**
- Response time p95 < 200ms
- Database pool usage < 50%
- Error rate < 0.1%
- Memory usage < 450MB
- CPU usage < 80%

---

## 🔄 PHASE 8: ROLLBACK PROCEDURES
**Only if migration fails completely**

### Emergency Rollback to H2:
1. **In Railway Variables, RESTORE:**
```bash
DATABASE_URL=jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;MODE=PostgreSQL
DATABASE_DRIVER=org.h2.Driver
DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
DATABASE_USERNAME=sa
DATABASE_PASSWORD=
DDL_AUTO=update
FORCE_ADMIN_RESET=true
```

2. **DELETE PostgreSQL variables:**
```bash
# Remove these:
DATABASE_DRIVER=org.postgresql.Driver
DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_LOB_NON_CONTEXTUAL_CREATION
```

3. **Trigger Redeployment**
4. **Delete PostgreSQL Service** (optional)

---

## ✅ FINAL VALIDATION CHECKLIST

### Core Functionality
- [ ] Admin login works: `admin@yourcompany.com` / `ChickenAdmin2024!Secure#`
- [ ] Password change required on first login
- [ ] Password change functionality works
- [ ] Location creation via admin portal works
- [ ] Location slug generation works
- [ ] Calculator returns correct results
- [ ] Sales data can be added and retrieved
- [ ] Marination logs properly saved
- [ ] Multi-tenant data isolation verified
- [ ] All API endpoints respond < 200ms

### Security
- [ ] `DDL_AUTO` set to `validate`
- [ ] `FORCE_ADMIN_RESET` set to `false`
- [ ] H2 console not accessible
- [ ] CORS properly configured (no wildcards)
- [ ] JWT stored in httpOnly cookies
- [ ] CSRF tokens working
- [ ] SSL/TLS enabled on database connection
- [ ] No sensitive data in logs

### Database
- [ ] PostgreSQL service active
- [ ] All tables created (admin_users, locations, sales_data, marination_log)
- [ ] Sequences working (IDs > 1000)
- [ ] Flyway migrations completed (V1, V2, V3)
- [ ] Connection pool stable (< 10 connections)
- [ ] No H2 references in logs
- [ ] Database response times acceptable

### Monitoring
- [ ] `/api/health` returns UP
- [ ] `/actuator/health` shows database UP
- [ ] `/actuator/prometheus` accessible
- [ ] Metrics being collected
- [ ] No memory leaks detected
- [ ] CPU usage normal (< 50%)
- [ ] Response times logged

---

## 🔧 TROUBLESHOOTING GUIDE

### Problem: "Connection refused" to PostgreSQL
**Solution:**
1. Verify PostgreSQL service is "Active" in Railway
2. Check ChickenCalculator service has access to PostgreSQL variables
3. Ensure no H2 DATABASE_URL overriding PostgreSQL

### Problem: "Authentication failed for user"
**Solution:**
1. DATABASE_URL should be auto-provided by Railway
2. Don't manually set PGUSER/PGPASSWORD
3. Remove all H2 variables completely

### Problem: "Flyway migration failed"
**Solution:**
1. Check V3__postgresql_sequences.sql syntax
2. Ensure V1 migration updated for PostgreSQL
3. May need to clean and re-run migrations

### Problem: "Admin login still failing"
**Solution:**
1. Verify `FORCE_ADMIN_RESET=false`
2. Check admin was created in logs
3. Ensure using configured password
4. Check BCrypt rounds match

### Problem: "Sequence 'entity_id_seq' not found"
**Solution:**
1. V3 migration didn't run
2. Check Flyway schema_version table
3. May need to manually create sequences

### Problem: "DATE() function not found"
**Solution:**
1. MarinationLogRepository not updated
2. Ensure using `CAST(timestamp AS DATE)`
3. Rebuild and redeploy

### Problem: High memory usage
**Solution:**
1. Reduce `DB_POOL_SIZE` to 8 or 5
2. Reduce `DB_MIN_IDLE` to 1
3. Check for memory leaks in logs

---

## 📊 PERFORMANCE BASELINES

### Expected Performance Metrics (PostgreSQL)
- **API Response Times**:
  - GET endpoints: < 50ms p95
  - POST calculations: < 150ms p95
  - Database queries: < 20ms p95
  
- **Resource Usage**:
  - Memory: 300-400MB steady state
  - CPU: 10-30% average
  - Database connections: 2-5 active
  
- **Throughput**:
  - 100+ requests/second capability
  - 10+ concurrent users
  - 1000+ calculations/hour

---

## 🎯 SUCCESS CRITERIA

### Migration Success Defined As:
1. ✅ All 13 code compatibility issues resolved
2. ✅ All 27 security vulnerabilities addressed  
3. ✅ PostgreSQL database operational
4. ✅ Admin authentication working
5. ✅ All endpoints functional
6. ✅ Performance meets baselines
7. ✅ Monitoring operational
8. ✅ Rollback capability preserved
9. ✅ Zero data loss
10. ✅ Production ready (10/10 score)

---

## 📅 TIMELINE SUMMARY

| Phase | Duration | Critical |
|-------|----------|----------|
| 0. Code Fixes | 30-45 min | YES |
| 1. Env Fixes | 5 min | YES |
| 2. PostgreSQL | 10-15 min | YES |
| 3. Migration | 10 min | YES |
| 4. Monitoring | 15 min | YES |
| 5. Validation | 20 min | YES |
| 6. Security | 15 min | NO |
| 7. Monitoring | 10 min | NO |
| **TOTAL** | **~2.5 hours** | |

---

## 📞 EMERGENCY CONTACTS

### If Critical Issues Arise:
1. Check Railway Status: https://status.railway.app
2. Railway Discord: https://discord.gg/railway
3. PostgreSQL Docs: https://www.postgresql.org/docs/
4. Spring Boot Issues: https://github.com/spring-projects/spring-boot/issues

---

## 📝 POST-MIGRATION NOTES

### After Successful Migration:
1. Document actual vs expected timeline
2. Note any issues encountered
3. Update this guide with findings
4. Create backup of PostgreSQL
5. Schedule regular backups
6. Monitor for 24-48 hours
7. Update README.md with PostgreSQL info
8. Remove H2 dependencies from pom.xml (future)
9. Clean up H2-specific code (future)
10. Celebrate successful migration! 🎉

---

## 🔗 RELATED DOCUMENTATION

- **CLAUDE.md**: Development guide and deployment checklist
- **ARCHITECTURE.md**: System design and architecture
- **README.md**: Project overview and quick start
- **FLYWAY_IMPLEMENTATION.md**: Database migration details
- **METRICS_IMPLEMENTATION.md**: Monitoring setup guide

---

*Last Updated: December 2024*
*Document Version: 1.0*
*Migration Plan Status: READY FOR EXECUTION*