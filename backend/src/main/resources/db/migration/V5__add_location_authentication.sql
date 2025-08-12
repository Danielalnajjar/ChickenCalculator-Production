-- V5: Add location authentication fields
-- This migration adds authentication capabilities to locations for multi-tenant access control

-- Add authentication fields to locations table
ALTER TABLE locations ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE locations ADD COLUMN IF NOT EXISTS requires_auth BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE locations ADD COLUMN IF NOT EXISTS session_timeout_hours INTEGER NOT NULL DEFAULT 8;
ALTER TABLE locations ADD COLUMN IF NOT EXISTS last_password_change TIMESTAMP;
ALTER TABLE locations ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE locations ADD COLUMN IF NOT EXISTS last_failed_login TIMESTAMP;

-- Set a default password for existing locations (password: "ChangeMe123!")
-- This uses BCrypt with 10 rounds
-- Generated hash for "ChangeMe123!" using BCrypt
UPDATE locations 
SET password_hash = '$2a$10$ZH7XRVxRNWxsU.YLNxKfCOPwLGX4MKfGl9oJNB8Kg1.pVHVqg/h2e'
WHERE password_hash IS NULL;

-- Create index for faster slug lookups (used in authentication)
CREATE INDEX IF NOT EXISTS idx_locations_slug ON locations(slug);

-- Create index for authentication status checks
CREATE INDEX IF NOT EXISTS idx_locations_auth_status ON locations(slug, status, requires_auth);

-- Note: We're keeping is_default for now to ensure backward compatibility
-- It will be removed in a future migration after full deployment