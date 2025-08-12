-- Reset admin password to default for recovery
-- This migration will reset the admin password to the default ADMIN_DEFAULT_PASSWORD
-- The password will be hashed using BCrypt with 10 rounds
-- BCrypt hash for 'Admin123!' with 10 rounds (you'll need to use the actual password from ADMIN_DEFAULT_PASSWORD)

-- Delete existing admin users (there should only be one)
DELETE FROM admin_users;

-- Note: The AdminService will recreate the admin user with the correct password from environment variable
-- on next startup after this migration runs