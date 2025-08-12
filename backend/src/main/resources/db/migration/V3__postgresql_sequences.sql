-- PostgreSQL sequences migration
-- This migration creates sequences for all entities and configures them properly

-- Create the main sequence that will be used by all entities
CREATE SEQUENCE IF NOT EXISTS entity_id_seq START WITH 1 INCREMENT BY 1;

-- Update existing tables to use the sequence
-- Admin Users table
SELECT setval('entity_id_seq', COALESCE((SELECT MAX(id) FROM admin_users), 1), false);

-- Locations table  
SELECT setval('entity_id_seq', COALESCE((SELECT MAX(id) FROM locations), 1), false);

-- Sales Data table
SELECT setval('entity_id_seq', COALESCE((SELECT MAX(id) FROM sales_data), 1), false);

-- Marination Log table
SELECT setval('entity_id_seq', COALESCE((SELECT MAX(id) FROM marination_log), 1), false);

-- Set the sequence to start from the highest existing ID + 1
SELECT setval('entity_id_seq', 
    GREATEST(
        COALESCE((SELECT MAX(id) FROM admin_users), 0),
        COALESCE((SELECT MAX(id) FROM locations), 0),
        COALESCE((SELECT MAX(id) FROM sales_data), 0),
        COALESCE((SELECT MAX(id) FROM marination_log), 0)
    ) + 1
);

-- Note: The JPA entities now use @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
-- with @SequenceGenerator(name = "entity_seq", sequenceName = "entity_id_seq", allocationSize = 1)
-- This ensures PostgreSQL compatibility and proper ID generation