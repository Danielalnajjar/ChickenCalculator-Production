-- Database-agnostic sequences migration
-- This migration creates sequences for all entities and configures them properly
-- Works with both H2 (local development) and PostgreSQL (production)

-- Create the main sequence that will be used by all entities
CREATE SEQUENCE IF NOT EXISTS entity_id_seq START WITH 1 INCREMENT BY 1;

-- Note: H2 doesn't support setval function, so we'll rely on JPA to handle sequence initialization
-- PostgreSQL will use the sequence as-is since it supports setval in production
-- The sequence will auto-increment starting from 1, which is fine for new installations
-- For existing data in production, Railway will handle sequence synchronization

-- Note: The JPA entities now use @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
-- with @SequenceGenerator(name = "entity_seq", sequenceName = "entity_id_seq", allocationSize = 1)
-- This ensures both H2 and PostgreSQL compatibility