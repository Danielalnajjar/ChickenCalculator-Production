# Flyway Database Migrations

This directory contains Flyway database migration scripts for the ChickenCalculator application.

## Migration Files

### V1__initial_schema.sql
- **Purpose**: Creates the initial database schema with all tables, indexes, and constraints
- **Tables Created**:
  - `admin_users` - System administrators with authentication
  - `locations` - Calculator locations with slug-based routing  
  - `sales_data` - Historical sales data (location-scoped)
  - `marination_log` - Marination history (location-scoped)
- **Default Data**: Creates the default "Main Calculator" location
- **Indexes**: Performance optimization indexes for common queries

### V2__add_indexes_and_constraints.sql
- **Purpose**: Template for future schema changes
- **Status**: Placeholder - contains no actual migrations yet

## Configuration

### Development (application.yml)
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: true
```

### Production (application-production.yml)
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: true
    clean-disabled: true  # Safety measure for production
```

## Migration Strategy

### Existing Databases
- **baseline-on-migrate**: `true` - Allows Flyway to work with existing databases
- **baseline-version**: `0` - Sets baseline for existing data
- **validate-on-migrate**: `true` - Ensures migration integrity

### New Databases
- Flyway will run all migrations from V1 onwards
- Creates clean schema with all necessary tables and constraints

## Schema Management

### Before Flyway (Legacy)
- Used Hibernate DDL auto-update (`ddl-auto: update`)
- Risk of data loss and schema drift

### With Flyway (Current)
- Version-controlled schema changes
- Safe production deployments
- Schema validation and rollback capabilities
- Change tracking in `flyway_schema_history` table

## Best Practices

1. **Never modify existing migration files** - Create new migrations instead
2. **Use descriptive migration names** - Follow pattern `V{version}__{description}.sql`
3. **Test migrations thoroughly** - Verify both forward and rollback scenarios
4. **Keep migrations atomic** - Each migration should be a complete, independent change
5. **Document complex changes** - Add comments explaining non-obvious modifications

## Data Safety

### Production Safeguards
- `clean-disabled: true` - Prevents accidental data deletion
- `validate-on-migrate: true` - Ensures migration integrity
- Hibernate `ddl-auto: validate` - Prevents automatic schema changes

### Default Data Management
- Admin users created via `AdminService.initializeDefaultAdmin()`
- Default location created via Flyway V1 migration
- `DefaultDataInitializer` checks for existing data to prevent duplicates

## Troubleshooting

### Common Issues

1. **Baseline Required**
   - Error: "Found non-empty schema(s) without schema history table"
   - Solution: Flyway will automatically baseline with `baseline-on-migrate: true`

2. **Schema Validation Errors**
   - Error: "Validate failed: Migration checksum mismatch"
   - Solution: Check if migration files were modified after execution

3. **Duplicate Key Errors**
   - Error: Unique constraint violation during migration
   - Solution: Check for existing data, especially default records

### Manual Flyway Operations

```bash
# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Manual migration (if needed)
mvn flyway:migrate

# Baseline existing database (one-time operation)
mvn flyway:baseline
```

## Environment Variables

- `FLYWAY_ENABLED` - Enable/disable Flyway (default: true)
- `FLYWAY_BASELINE_ON_MIGRATE` - Auto-baseline for existing databases
- `FLYWAY_LOCATIONS` - Migration file locations
- `FLYWAY_VALIDATE_ON_MIGRATE` - Enable migration validation

## Integration with Application

### Startup Sequence
1. Flyway runs migrations (if enabled)
2. Hibernate validates schema against entities
3. `AdminService.initializeDefaultAdmin()` creates admin user
4. `DefaultDataInitializer.initializeDefaultLocation()` checks for default location
5. Application starts accepting requests

### Entity Changes
- New fields: Add migration to alter table
- Constraints: Add migration for new constraints
- Indexes: Add migration for performance improvements
- Data changes: Use migration scripts, not application code