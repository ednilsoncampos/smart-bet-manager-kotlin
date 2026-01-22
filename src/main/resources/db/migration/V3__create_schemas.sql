-- Smart Bet Manager - Create schemas for better organization
-- Version: 3.0.0

-- ============================================
-- Create schemas
-- ============================================
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS betting;
CREATE SCHEMA IF NOT EXISTS log;

-- ============================================
-- Move tables to core schema
-- ============================================
ALTER TABLE users SET SCHEMA core;
ALTER TABLE betting_providers SET SCHEMA core;
ALTER TABLE bankrolls SET SCHEMA core;
ALTER TABLE bankroll_transactions SET SCHEMA core;

-- ============================================
-- Move tables to betting schema
-- ============================================
ALTER TABLE bet_tickets SET SCHEMA betting;
ALTER TABLE bet_selections SET SCHEMA betting;

-- ============================================
-- Move tables to log schema
-- ============================================
-- Note: provider_api_requests table is not managed by JPA entities
-- It will remain in public schema for now
-- ALTER TABLE provider_api_requests SET SCHEMA log;

-- ============================================
-- Update foreign key references
-- ============================================
-- Note: PostgreSQL automatically updates FK references when moving tables to schemas
-- The references remain valid as long as the table names are unique across schemas

-- ============================================
-- Grant permissions (if needed for application user)
-- ============================================
-- GRANT USAGE ON SCHEMA core TO app_user;
-- GRANT USAGE ON SCHEMA betting TO app_user;
-- GRANT USAGE ON SCHEMA log TO app_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA core TO app_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA betting TO app_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA log TO app_user;
