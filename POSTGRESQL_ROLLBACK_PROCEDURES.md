# PostgreSQL Migration: Rollback & Disaster Recovery Procedures

**Document Version:** 1.0  
**Last Updated:** December 2024  
**System:** ChickenCalculator Production  
**Platform:** Railway (Project ID: 767deec0-30ac-4238-a57b-305f5470b318)

## Quick Reference Emergency Contacts

| Role | Contact | Availability |
|------|---------|-------------|
| Lead Developer | [PRIMARY_CONTACT] | 24/7 during migration |
| DevOps Engineer | [DEVOPS_CONTACT] | Business hours + on-call |
| Product Owner | [PRODUCT_CONTACT] | Business hours |
| Railway Support | support@railway.app | 24/7 (Pro plan) |

## Emergency Decision Tree

```
MIGRATION ISSUE DETECTED
│
├── Service Completely Down? 
│   ├── YES → Execute IMMEDIATE ROLLBACK (Section 3)
│   └── NO → Continue assessment
│
├── Authentication Broken?
│   ├── YES → Check severity → IMMEDIATE or PARTIAL rollback
│   └── NO → Continue assessment
│
├── Data Corruption Detected?
│   ├── YES → STOP ALL OPERATIONS → Execute Data Recovery (Section 5)
│   └── NO → Continue assessment
│
├── Performance < 50% baseline?
│   ├── YES → 30min fix window → Then rollback
│   └── NO → Monitor and fix
│
└── Other issues → Use specific procedures below
```

---

## 1. Migration Rollback Scenarios

### 1.1 PostgreSQL Deployment Fails

**Trigger Conditions:**
- Database connection errors on startup
- Flyway migration failures
- Connection timeout errors
- "Database not found" errors

**Immediate Actions (Execute within 5 minutes):**

```bash
# 1. Check Railway deployment logs
railway logs --tail 100

# 2. Verify environment variables
railway variables

# 3. Check database service status
railway status
```

**Rollback Procedure:**

1. **Revert Database Configuration** (2 minutes)
   ```bash
   # Remove PostgreSQL environment variables
   railway variables delete DATABASE_URL
   railway variables delete DATABASE_USERNAME  
   railway variables delete DATABASE_PASSWORD
   railway variables delete DATABASE_DRIVER
   railway variables delete DATABASE_PLATFORM
   railway variables delete DATABASE_DIALECT
   ```

2. **Deploy Previous Working Version** (3 minutes)
   ```bash
   # Rollback to last known good commit
   git log --oneline -10  # Find last working commit
   git checkout <LAST_WORKING_COMMIT>
   git push origin main --force
   ```

3. **Verify H2 Restoration** (2 minutes)
   - Check `/api/health` returns `UP`
   - Verify admin login works
   - Test calculator functionality

**Success Criteria:**
- [ ] Application starts successfully
- [ ] Health endpoint returns `UP` status  
- [ ] Admin portal accessible
- [ ] Database operations functional
- [ ] All existing data preserved

---

### 1.2 Flyway Migration Errors

**Trigger Conditions:**
- Flyway validation failures
- Schema version conflicts
- Migration checksum mismatches
- Incomplete migration states

**Rollback Procedure:**

1. **Assess Migration State** (1 minute)
   ```bash
   # Check Flyway history
   railway run mvn flyway:info
   railway run mvn flyway:validate
   ```

2. **Clean Migration State** (3 minutes)
   ```bash
   # Option A: Repair migration
   railway run mvn flyway:repair
   
   # Option B: Baseline and retry (if safe)
   railway run mvn flyway:baseline
   railway run mvn flyway:migrate
   
   # Option C: Full rollback to H2
   # Execute Section 1.1 if repair fails
   ```

3. **Data Integrity Check** (2 minutes)
   ```bash
   # Verify data consistency
   curl -X GET "https://chickencalculator-production-production-2953.up.railway.app/api/health"
   curl -X GET "https://chickencalculator-production-production-2953.up.railway.app/api/v1/admin/stats"
   ```

---

### 1.3 Application Won't Start

**Trigger Conditions:**
- Spring Boot startup failures
- Dependency injection errors
- Configuration binding errors
- Port binding failures

**Immediate Actions:**

1. **Check Application Logs** (1 minute)
   ```bash
   railway logs --tail 200
   ```

2. **Verify Environment Variables** (1 minute)
   ```bash
   railway variables list
   ```

3. **Execute Fast Rollback** (5 minutes)
   ```bash
   # Restore H2 configuration
   railway variables set DATABASE_URL="jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE"
   railway variables set DATABASE_USERNAME="sa"  
   railway variables set DATABASE_PASSWORD=""
   railway variables set DATABASE_DRIVER="org.h2.Driver"
   railway variables set DATABASE_PLATFORM="org.hibernate.dialect.H2Dialect"
   railway variables set DATABASE_DIALECT="org.hibernate.dialect.H2Dialect"
   
   # Force redeploy
   railway redeploy
   ```

---

### 1.4 Authentication Completely Broken

**Trigger Conditions:**
- JWT validation failures
- CSRF token issues
- Admin login impossible
- Session management errors

**Severity Assessment:**
- **Critical:** No admin access possible → IMMEDIATE rollback
- **High:** Some admin functions broken → PARTIAL rollback
- **Medium:** Limited functionality issues → 30-minute fix window

**Rollback Procedure:**

1. **Verify JWT Configuration** (2 minutes)
   ```bash
   # Check JWT secret exists
   railway variables get JWT_SECRET
   
   # Verify admin password
   railway variables get ADMIN_DEFAULT_PASSWORD
   ```

2. **Reset Authentication System** (3 minutes)
   ```bash
   # Generate new JWT secret
   NEW_JWT_SECRET=$(openssl rand -base64 48)
   railway variables set JWT_SECRET="$NEW_JWT_SECRET"
   
   # Reset admin password
   railway variables set ADMIN_DEFAULT_PASSWORD="TempAdmin123!"
   railway variables set FORCE_PASSWORD_CHANGE="true"
   ```

3. **If Authentication Still Fails** (5 minutes)
   - Execute full H2 rollback (Section 1.1)
   - Verify admin account in H2 database
   - Test complete authentication flow

---

### 1.5 Performance Severely Degraded

**Trigger Conditions:**
- Response times > 5x baseline (>1000ms)
- Database connection pool exhaustion
- Memory usage > 90%
- Error rate > 5%

**Performance Baselines:**
- Response time p95: < 200ms
- Database pool usage: < 50%
- Memory usage: < 512MB  
- Error rate: < 0.1%

**Rollback Decision Matrix:**
- **30-minute fix window:** If performance can be restored quickly
- **Immediate rollback:** If degradation affects > 50% of functionality

**Rollback Procedure:**

1. **Performance Analysis** (3 minutes)
   ```bash
   # Check metrics
   curl -s https://chickencalculator-production-production-2953.up.railway.app/actuator/metrics | grep -E "(hikaricp|jvm.memory|http.server)"
   
   # Check database connections
   railway logs --filter "HikariCP"
   ```

2. **Quick Fixes Attempt** (15 minutes max)
   ```bash
   # Increase database pool size
   railway variables set DB_POOL_SIZE="25"
   railway variables set DB_MAX_LIFETIME="900000"
   
   # Restart service
   railway redeploy --detach
   ```

3. **If No Improvement** (execute H2 rollback)

---

### 1.6 Data Corruption Detected

**Trigger Conditions:**
- Inconsistent data returned from API
- Flyway schema history corruption
- Foreign key violations
- Data integrity check failures

**CRITICAL: STOP ALL OPERATIONS IMMEDIATELY**

**Emergency Procedure:**

1. **Put System in Maintenance Mode** (1 minute)
   ```bash
   # Set maintenance mode
   railway variables set MAINTENANCE_MODE="true"
   railway redeploy --detach
   ```

2. **Assess Corruption Scope** (5 minutes)
   ```bash
   # Connect to database
   railway run psql
   
   # Check data integrity
   SELECT COUNT(*) FROM admin_users;
   SELECT COUNT(*) FROM locations;
   SELECT COUNT(*) FROM sales_data;
   SELECT COUNT(*) FROM marination_log;
   
   # Check foreign key constraints
   SELECT * FROM information_schema.table_constraints WHERE constraint_type = 'FOREIGN KEY';
   ```

3. **Execute Emergency Rollback** (10 minutes)
   - Restore from last known good backup (Section 5.2)
   - If no backup: complete H2 rollback with data loss acceptance
   - Document all affected data for recovery

---

## 2. Rollback Decision Matrix

### 2.1 Rollback Triggers

| Scenario | Automatic | Manual Decision | Approval Required |
|----------|-----------|-----------------|-------------------|
| Service completely down (>5 min) | ✓ | | |
| Authentication completely broken | ✓ | | |
| Data corruption detected | ✓ | | |
| Performance < 20% baseline | | ✓ | Lead Developer |
| Minor functionality issues | | ✓ | Product Owner |
| Security vulnerabilities | ✓ | | |

### 2.2 Rollback Window Timing

| Time After Migration | Action Required | Approval Level |
|---------------------|-----------------|----------------|
| 0-15 minutes | Immediate rollback allowed | Any team member |
| 15-60 minutes | Quick assessment required | Lead Developer |
| 1-4 hours | Business impact analysis | Lead + Product Owner |
| 4+ hours | Full incident process | All stakeholders |

### 2.3 Stakeholder Communication Plan

**Immediate Notification (within 5 minutes):**
```bash
# Slack notification template
MIGRATION_STATUS="ROLLBACK_INITIATED"
ISSUE="[Brief description]"
ETA="[Expected resolution time]"

# Send to #engineering-alerts channel
```

**Notification Escalation:**
1. **5 minutes:** Engineering team
2. **15 minutes:** Product owner
3. **30 minutes:** Management (if business impact)
4. **1 hour:** Executive team (if extended outage)

---

## 3. H2 Restoration Procedure

### 3.1 Complete H2 Restoration

**Prerequisites:**
- Railway CLI access
- GitHub repository access
- Backup verification completed

**Environment Variable Restoration:**

```bash
# 1. Remove PostgreSQL variables
railway variables delete DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD DATABASE_DRIVER DATABASE_PLATFORM DATABASE_DIALECT

# 2. Set H2 variables explicitly (optional - defaults work)
railway variables set DATABASE_URL="jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE"
railway variables set DATABASE_USERNAME="sa"
railway variables set DATABASE_PASSWORD=""
railway variables set DATABASE_DRIVER="org.h2.Driver"
railway variables set DATABASE_PLATFORM="org.hibernate.dialect.H2Dialect"
railway variables set DATABASE_DIALECT="org.hibernate.dialect.H2Dialect"

# 3. Ensure H2 console disabled in production
railway variables set H2_CONSOLE_ENABLED="false"
```

**Application Configuration Restoration:**

1. **Revert Application Profile** (1 minute)
   ```bash
   railway variables set SPRING_PROFILES_ACTIVE="production"
   ```

2. **Restore Flyway Configuration** (1 minute)
   ```bash
   # Flyway will work with H2 using PostgreSQL compatibility mode
   # Existing migrations will apply correctly
   ```

3. **Deploy Previous Version** (3 minutes)
   ```bash
   # Find last working commit
   git log --oneline --grep="PostgreSQL" -n 10
   
   # Checkout pre-migration state
   git checkout <PRE_MIGRATION_COMMIT>
   git push origin main --force
   ```

### 3.2 H2 Database File Recovery

**If H2 database file is corrupted:**

1. **Check File System** (2 minutes)
   ```bash
   railway run ls -la /app/data/
   railway run du -sh /app/data/*
   ```

2. **Restore from Volume Backup** (if available)
   ```bash
   # Railway volume restoration (if configured)
   railway volumes list
   railway volumes restore <VOLUME_ID> <BACKUP_ID>
   ```

3. **Initialize Fresh Database** (5 minutes)
   ```bash
   # Remove corrupted database files
   railway run rm -rf /app/data/chicken-calculator-db*
   
   # Restart application (will create fresh database)
   railway restart
   
   # Run Flyway migrations
   railway run mvn flyway:migrate
   ```

### 3.3 Data Consistency Verification

**Post-Restoration Checks:**

```bash
# 1. Health check
curl -f https://chickencalculator-production-production-2953.up.railway.app/api/health

# 2. Database connectivity
curl -f https://chickencalculator-production-production-2953.up.railway.app/actuator/health

# 3. Admin functionality
curl -X POST https://chickencalculator-production-production-2953.up.railway.app/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@yourcompany.com","password":"[admin_password]"}'

# 4. Calculator functionality
curl -X POST https://chickencalculator-production-production-2953.up.railway.app/api/v1/calculator/calculate \
  -H "Content-Type: application/json" \
  -d '{"inventory":{"pansSoy":10,"pansTeriyaki":10,"pansTurmeric":10},"projectedSales":{"day0":100,"day1":100,"day2":100,"day3":100},"availableRawChickenKg":50,"alreadyMarinatedSoy":0,"alreadyMarinatedTeriyaki":0,"alreadyMarinatedTurmeric":0}'

# 5. Data integrity checks
curl -f https://chickencalculator-production-production-2953.up.railway.app/api/v1/admin/stats
```

---

## 4. Partial Rollback Options

### 4.1 Keep PostgreSQL, Fix Configuration

**When to Use:**
- PostgreSQL is working but configuration issues exist
- Performance issues that can be tuned
- Minor connection problems

**Procedure:**

1. **Database Tuning** (10 minutes)
   ```bash
   # Adjust connection pool settings
   railway variables set DB_POOL_SIZE="20"
   railway variables set DB_CONNECTION_TIMEOUT="20000"
   railway variables set DB_IDLE_TIMEOUT="600000"
   railway variables set DB_MAX_LIFETIME="1200000"
   
   # Enable connection monitoring
   railway variables set DB_LEAK_THRESHOLD="30000"
   ```

2. **JPA Configuration Adjustment** (5 minutes)
   ```bash
   # Ensure correct PostgreSQL dialect
   railway variables set DATABASE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
   railway variables set DATABASE_PLATFORM="org.hibernate.dialect.PostgreSQLDialect"
   ```

3. **Flyway Adjustment** (3 minutes)
   ```bash
   # Reset Flyway if needed
   railway run mvn flyway:repair
   railway run mvn flyway:validate
   ```

### 4.2 Temporary H2 Parallel Operation

**When to Use:**
- Need immediate service restoration
- PostgreSQL investigation required
- Gradual migration approach needed

**Setup Dual Configuration:**

1. **Create H2 Fallback Service** (15 minutes)
   ```bash
   # Deploy separate H2 instance for fallback
   railway service create chicken-calc-h2
   
   # Configure H2-only environment
   railway variables set --service chicken-calc-h2 DATABASE_URL="jdbc:h2:file:/app/data/chicken-calculator-db"
   railway variables set --service chicken-calc-h2 DATABASE_DRIVER="org.h2.Driver"
   
   # Update domain routing
   railway domain create chicken-calc-h2 --suffix h2
   ```

2. **Traffic Routing Configuration:**
   ```bash
   # Use Railway's load balancer or custom routing
   # Route critical traffic to H2 instance
   # Route test traffic to PostgreSQL instance
   ```

### 4.3 Read-Only Mode Activation

**When to Use:**
- Data integrity concerns
- Investigation needed while maintaining service
- Prevents further data corruption

**Implementation:**

1. **Application Configuration** (3 minutes)
   ```bash
   railway variables set READ_ONLY_MODE="true"
   railway variables set MAINTENANCE_MESSAGE="System under maintenance - read-only mode"
   ```

2. **Database Configuration** (2 minutes)
   ```bash
   # Create read-only database user (PostgreSQL)
   railway run psql -c "CREATE ROLE readonly_user WITH LOGIN PASSWORD 'readonly_pass';"
   railway run psql -c "GRANT CONNECT ON DATABASE chicken_calculator TO readonly_user;"
   railway run psql -c "GRANT USAGE ON SCHEMA public TO readonly_user;"
   railway run psql -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;"
   
   # Switch to read-only user
   railway variables set DATABASE_USERNAME="readonly_user"
   railway variables set DATABASE_PASSWORD="readonly_pass"
   ```

### 4.4 Feature Flag Management

**Gradual Rollback Strategy:**

1. **Disable New Features** (1 minute)
   ```bash
   railway variables set ENABLE_MULTI_TENANT="false"
   railway variables set ENABLE_ADVANCED_CALCULATIONS="false"
   railway variables set ENABLE_NEW_API="false"
   ```

2. **Progressive Feature Restoration:**
   - Monitor each feature individually
   - Enable features one by one
   - Verify stability at each step

---

## 5. Data Recovery Procedures

### 5.1 PostgreSQL Backup Restoration

**Prerequisites:**
- PostgreSQL backup availability
- Database access credentials
- Maintenance window scheduled

**Backup Location Verification:**
```bash
# Check Railway volume backups
railway volumes list
railway volumes backups list <VOLUME_ID>

# Check external backup service
# (Configure according to your backup strategy)
```

**Restoration Procedure:**

1. **Stop Application** (1 minute)
   ```bash
   railway variables set MAINTENANCE_MODE="true"
   railway redeploy --detach
   ```

2. **Connect to Database** (2 minutes)
   ```bash
   railway run psql $DATABASE_URL
   ```

3. **Restore from Backup** (10-30 minutes, depending on size)
   ```bash
   # Drop existing database (CAUTION!)
   railway run psql -c "DROP DATABASE IF EXISTS chicken_calculator;"
   railway run psql -c "CREATE DATABASE chicken_calculator;"
   
   # Restore from backup
   railway run pg_restore --verbose --clean --no-acl --no-owner \
     -h <host> -U <username> -d chicken_calculator <backup_file>
   ```

4. **Verify Data Integrity** (5 minutes)
   ```sql
   -- Connect to restored database
   \c chicken_calculator
   
   -- Check table counts
   SELECT 'admin_users' as table_name, COUNT(*) FROM admin_users
   UNION ALL
   SELECT 'locations', COUNT(*) FROM locations  
   UNION ALL
   SELECT 'sales_data', COUNT(*) FROM sales_data
   UNION ALL
   SELECT 'marination_log', COUNT(*) FROM marination_log;
   
   -- Check data consistency
   SELECT l.name, COUNT(s.id) as sales_records 
   FROM locations l 
   LEFT JOIN sales_data s ON l.id = s.location_id 
   GROUP BY l.name;
   ```

5. **Restart Application** (3 minutes)
   ```bash
   railway variables delete MAINTENANCE_MODE
   railway redeploy
   ```

### 5.2 Point-in-Time Recovery

**When to Use:**
- Data corruption at specific time identified
- Need to restore to pre-corruption state
- Partial data loss acceptable

**Procedure:**

1. **Identify Recovery Point** (5 minutes)
   ```bash
   # Check application logs for last known good state
   railway logs --since "2024-12-12T10:00:00Z" --until "2024-12-12T12:00:00Z"
   
   # Identify exact timestamp before corruption
   TARGET_TIMESTAMP="2024-12-12T11:30:00Z"
   ```

2. **Perform Point-in-Time Restore** (15 minutes)
   ```bash
   # Stop application
   railway variables set MAINTENANCE_MODE="true"
   
   # Restore to specific timestamp
   railway run pg_restore --verbose --clean --no-acl --no-owner \
     --if-exists --exit-on-error \
     -h <host> -U <username> -d chicken_calculator \
     --timestamp="$TARGET_TIMESTAMP" <backup_file>
   ```

### 5.3 Data Export/Import Processes

**Emergency Data Preservation:**

1. **Export Critical Data** (5 minutes)
   ```bash
   # Export user data
   railway run psql -c "COPY admin_users TO STDOUT WITH CSV HEADER" > admin_users_backup.csv
   
   # Export business data
   railway run psql -c "COPY locations TO STDOUT WITH CSV HEADER" > locations_backup.csv
   railway run psql -c "COPY sales_data TO STDOUT WITH CSV HEADER" > sales_data_backup.csv
   railway run psql -c "COPY marination_log TO STDOUT WITH CSV HEADER" > marination_log_backup.csv
   ```

2. **Import to H2 Database** (10 minutes)
   ```bash
   # Switch to H2 configuration
   railway variables set DATABASE_URL="jdbc:h2:file:/app/data/chicken-calculator-db"
   railway variables set DATABASE_DRIVER="org.h2.Driver"
   
   # Restart with H2
   railway redeploy
   
   # Import data (using application endpoints or direct H2 console)
   # Note: May require data transformation due to type differences
   ```

### 5.4 Schema Recreation

**Complete Schema Rebuild:**

1. **Backup Current Schema** (2 minutes)
   ```bash
   railway run pg_dump --schema-only $DATABASE_URL > schema_backup.sql
   ```

2. **Recreate from Flyway** (5 minutes)
   ```bash
   # Drop all objects
   railway run psql -c "DROP SCHEMA public CASCADE;"
   railway run psql -c "CREATE SCHEMA public;"
   
   # Run Flyway migrations
   railway run mvn flyway:migrate
   ```

3. **Verify Schema** (2 minutes)
   ```bash
   railway run psql -c "\d+"  # List all tables and structures
   ```

---

## 6. Emergency Procedures

### 6.1 Total Service Failure

**Definition:** Complete system unavailability, no HTTP responses

**Emergency Response (Execute within 2 minutes):**

1. **Immediate Assessment:**
   ```bash
   # Check service status
   curl -I https://chickencalculator-production-production-2953.up.railway.app/api/health
   
   # Check Railway status
   railway status
   
   # Check deployment logs
   railway logs --tail 50
   ```

2. **Emergency Rollback:**
   ```bash
   # Rollback to last known working commit
   git log --oneline -5
   git checkout <LAST_WORKING_COMMIT>
   git push origin main --force
   
   # If still failing, rollback environment
   railway variables set DATABASE_URL="jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE"
   railway variables set DATABASE_DRIVER="org.h2.Driver"
   railway redeploy
   ```

3. **Escalation:**
   - Notify all stakeholders immediately
   - Contact Railway support if platform issue suspected
   - Activate disaster recovery team

### 6.2 Data Breach Detected

**Immediate Response (Execute within 1 minute):**

1. **Isolation:**
   ```bash
   # Put system in maintenance mode
   railway variables set MAINTENANCE_MODE="true"
   railway variables set DISABLE_ALL_ENDPOINTS="true"
   railway redeploy
   ```

2. **Assessment:**
   ```bash
   # Check for suspicious activity
   railway logs --filter "ERROR\|WARN\|security" --tail 200
   
   # Check admin user access logs
   railway run psql -c "SELECT * FROM admin_users ORDER BY last_login DESC LIMIT 10;"
   ```

3. **Containment:**
   ```bash
   # Rotate all secrets immediately
   NEW_JWT_SECRET=$(openssl rand -base64 48)
   railway variables set JWT_SECRET="$NEW_JWT_SECRET"
   
   # Reset admin passwords
   railway variables set FORCE_PASSWORD_CHANGE="true"
   railway variables set ADMIN_DEFAULT_PASSWORD="EmergencyReset123!"
   ```

### 6.3 Cascade Failure Prevention

**When multiple components failing:**

1. **Identify Root Cause:**
   - Database connection issues
   - Memory exhaustion
   - Network problems
   - Configuration conflicts

2. **Circuit Breaker Activation:**
   ```bash
   # Enable circuit breakers
   railway variables set ENABLE_CIRCUIT_BREAKER="true"
   railway variables set CIRCUIT_BREAKER_THRESHOLD="50"
   
   # Reduce resource consumption
   railway variables set DB_POOL_SIZE="5"
   railway variables set MAX_CONNECTIONS="10"
   ```

3. **Graceful Degradation:**
   ```bash
   # Disable non-essential features
   railway variables set DISABLE_METRICS="true"
   railway variables set DISABLE_LOGGING="false"  # Keep logging for debugging
   railway variables set READ_ONLY_MODE="true"
   ```

### 6.4 Emergency Maintenance Mode

**Activation Procedure:**

1. **Enable Maintenance Mode** (30 seconds)
   ```bash
   railway variables set MAINTENANCE_MODE="true"
   railway variables set MAINTENANCE_MESSAGE="System under emergency maintenance. ETA: [TIME]"
   railway redeploy --detach
   ```

2. **Customer Communication** (2 minutes)
   ```bash
   # Update status page (if available)
   # Send notification emails
   # Update social media/support channels
   ```

3. **Parallel Investigation:**
   - Clone production environment for testing
   - Investigate issues without affecting production
   - Prepare fixes in staging environment

---

## 7. Post-Incident Procedures

### 7.1 Root Cause Analysis

**Required Documentation:**

1. **Incident Timeline** (within 24 hours)
   ```markdown
   ## Incident Timeline
   
   | Time | Event | Action Taken | Result |
   |------|-------|--------------|--------|
   | 14:00 | Migration started | Applied PostgreSQL config | Success |
   | 14:15 | Connection errors | Checked logs | Errors identified |
   | 14:20 | Rollback initiated | Reverted to H2 | Service restored |
   ```

2. **Technical Analysis** (within 48 hours)
   - Root cause identification
   - Contributing factors
   - System behavior analysis
   - Performance impact assessment

3. **Impact Assessment** (within 72 hours)
   - Downtime duration
   - Affected users
   - Business impact
   - Data integrity status

### 7.2 Incident Documentation Template

```markdown
# Incident Report: PostgreSQL Migration Rollback

**Incident ID:** INC-2024-PGSQL-001
**Date:** [DATE]
**Duration:** [START_TIME] - [END_TIME]
**Severity:** [HIGH/MEDIUM/LOW]

## Summary
Brief description of what happened and impact.

## Timeline
[Detailed timeline from incident detection to resolution]

## Root Cause
[Technical root cause analysis]

## Resolution
[Steps taken to resolve the incident]

## Lessons Learned
[What we learned and will do differently]

## Action Items
- [ ] [Action item 1] - Owner: [NAME] - Due: [DATE]
- [ ] [Action item 2] - Owner: [NAME] - Due: [DATE]
```

### 7.3 Process Improvements

**Mandatory Reviews:**

1. **Technical Process Review** (within 1 week)
   - Migration procedure validation
   - Rollback procedure effectiveness
   - Monitoring and alerting gaps
   - Testing coverage improvements

2. **Communication Review** (within 1 week)
   - Stakeholder notification effectiveness
   - Documentation accuracy
   - Decision-making process
   - Escalation procedure review

### 7.4 Testing Enhancements

**Required Improvements:**

1. **Enhanced Staging Environment**
   ```bash
   # Create production-like staging
   railway environment create staging
   railway variables copy production staging
   
   # Test migration procedures
   # Validate rollback procedures
   # Performance testing
   ```

2. **Automated Testing**
   ```bash
   # Migration testing
   mvn test -Dtest=FlywayMigrationTest
   
   # Rollback testing
   mvn test -Dtest=DatabaseRollbackTest
   
   # End-to-end testing
   npm run test:e2e
   ```

### 7.5 Knowledge Base Updates

**Documentation Updates Required:**

1. **Update This Document**
   - Add new scenarios discovered
   - Refine procedures based on experience
   - Update contact information
   - Add new troubleshooting steps

2. **Training Materials**
   - Create rollback procedure training
   - Update runbooks
   - Share lessons learned
   - Conduct team training sessions

---

## Appendix A: Configuration Files

### A.1 H2 Configuration (Rollback State)

**application-production.yml (H2 Configuration):**
```yaml
spring:
  datasource:
    url: jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: true
    baseline-on-migrate: true
  h2:
    console:
      enabled: false  # Always disabled in production
```

### A.2 Environment Variables Reference

**Critical Environment Variables:**
```bash
# Database Configuration
DATABASE_URL=jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
DATABASE_USERNAME=sa
DATABASE_PASSWORD=""
DATABASE_DRIVER=org.h2.Driver
DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
DATABASE_DIALECT=org.hibernate.dialect.H2Dialect

# Security
JWT_SECRET=[32+ character secret]
ADMIN_DEFAULT_PASSWORD=[secure password]
H2_CONSOLE_ENABLED=false
FORCE_PASSWORD_CHANGE=true

# Application
SPRING_PROFILES_ACTIVE=production
PORT=8080

# Monitoring
SENTRY_DSN=[sentry project dsn]
```

---

## Appendix B: Emergency Scripts

### B.1 Quick Rollback Script

```bash
#!/bin/bash
# quick_rollback.sh - Emergency H2 rollback

echo "🚨 EMERGENCY ROLLBACK TO H2 🚨"
echo "Starting rollback at $(date)"

# Set H2 configuration
echo "Setting H2 environment variables..."
railway variables set DATABASE_URL="jdbc:h2:file:/app/data/chicken-calculator-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE"
railway variables set DATABASE_USERNAME="sa"
railway variables set DATABASE_PASSWORD=""  
railway variables set DATABASE_DRIVER="org.h2.Driver"
railway variables set DATABASE_PLATFORM="org.hibernate.dialect.H2Dialect"
railway variables set DATABASE_DIALECT="org.hibernate.dialect.H2Dialect"

# Remove PostgreSQL variables
echo "Removing PostgreSQL variables..."
railway variables delete DATABASE_HOST DATABASE_PORT DATABASE_NAME || true

# Force redeploy
echo "Redeploying application..."
railway redeploy --detach

echo "✅ Rollback initiated at $(date)"
echo "⏳ Waiting for deployment to complete..."
echo "🔍 Monitor logs with: railway logs --tail"
echo "🏥 Check health at: https://chickencalculator-production-production-2953.up.railway.app/api/health"
```

### B.2 Health Check Script

```bash
#!/bin/bash
# health_check.sh - Comprehensive health verification

URL="https://chickencalculator-production-production-2953.up.railway.app"

echo "🔍 COMPREHENSIVE HEALTH CHECK"
echo "=============================="

# Basic health check
echo "1. Basic Health Check..."
if curl -s -f "$URL/api/health" > /dev/null; then
    echo "   ✅ Health endpoint responding"
else
    echo "   ❌ Health endpoint failed"
    exit 1
fi

# Detailed health check
echo "2. Detailed Health Check..."
HEALTH_RESPONSE=$(curl -s "$URL/actuator/health")
echo "   Response: $HEALTH_RESPONSE"

# Database connectivity
echo "3. Database Connectivity..."
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    echo "   ✅ Database connection healthy"
else
    echo "   ❌ Database connection issues detected"
fi

# Admin authentication
echo "4. Admin Authentication..."
CSRF_TOKEN=$(curl -s "$URL/api/v1/admin/auth/csrf-token" | jq -r '.token')
if [ "$CSRF_TOKEN" != "null" ] && [ -n "$CSRF_TOKEN" ]; then
    echo "   ✅ CSRF token generation working"
else
    echo "   ❌ CSRF token generation failed"
fi

# Calculator functionality
echo "5. Calculator Functionality..."
CALC_RESPONSE=$(curl -s -X POST "$URL/api/v1/calculator/calculate" \
  -H "Content-Type: application/json" \
  -d '{"inventory":{"pansSoy":10,"pansTeriyaki":10,"pansTurmeric":10},"projectedSales":{"day0":100,"day1":100,"day2":100,"day3":100},"availableRawChickenKg":50,"alreadyMarinatedSoy":0,"alreadyMarinatedTeriyaki":0,"alreadyMarinatedTurmeric":0}')

if echo "$CALC_RESPONSE" | grep -q '"success":\s*true'; then
    echo "   ✅ Calculator endpoint working"
else
    echo "   ❌ Calculator endpoint failed"
    echo "   Response: $CALC_RESPONSE"
fi

echo "=============================="
echo "🏁 Health check completed at $(date)"
```

---

## Appendix C: Emergency Contact Information

### C.1 Technical Contacts

| Service | Contact Method | Response Time |
|---------|---------------|---------------|
| Railway Support | support@railway.app | 2-4 hours |
| GitHub Support | support@github.com | 4-8 hours |
| Domain/DNS | [DNS_PROVIDER] | [RESPONSE_TIME] |

### C.2 Business Contacts

| Role | Primary | Secondary | After Hours |
|------|---------|-----------|-------------|
| Product Owner | [EMAIL] | [PHONE] | [EMERGENCY_CONTACT] |
| Engineering Manager | [EMAIL] | [PHONE] | [EMERGENCY_CONTACT] |
| CTO | [EMAIL] | [PHONE] | [EMERGENCY_CONTACT] |

### C.3 Communication Channels

| Channel | Purpose | Access |
|---------|---------|--------|
| #engineering-alerts | Technical incidents | Slack |
| #product-updates | Business impact | Slack |
| Emergency Hotline | Critical incidents | [PHONE] |

---

**Document Revision History:**
- v1.0 (December 2024): Initial comprehensive rollback procedures
- Next review: January 2025

**Remember:** In emergency situations, customer safety and data integrity are the top priorities. When in doubt, choose the most conservative rollback approach.