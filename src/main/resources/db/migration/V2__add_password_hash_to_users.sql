-- Smart Bet Manager - Add password_hash to users
-- Version: 2.0.0

-- Add password_hash column for JWT authentication
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);

-- Update created_at and updated_at to use BIGINT (milliseconds timestamp)
-- for better compatibility with Kotlin Long type
ALTER TABLE users 
    ALTER COLUMN created_at TYPE BIGINT USING EXTRACT(EPOCH FROM created_at) * 1000,
    ALTER COLUMN updated_at TYPE BIGINT USING EXTRACT(EPOCH FROM updated_at) * 1000;

-- Set default values for new records
ALTER TABLE users 
    ALTER COLUMN created_at SET DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    ALTER COLUMN updated_at SET DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
