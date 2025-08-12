-- V2 Migration - Add additional indexes and constraints for performance optimization
-- This migration is a placeholder for future schema changes

-- Example: Add additional indexes for performance
-- CREATE INDEX idx_admin_users_email ON admin_users(email);
-- CREATE INDEX idx_locations_slug ON locations(slug);

-- Example: Add additional constraints
-- ALTER TABLE locations ADD CONSTRAINT chk_location_email 
--   CHECK (manager_email ~ '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+[.][A-Za-z]+$');

-- Note: This file serves as a template for future migrations
-- Remove this comment when adding actual migrations