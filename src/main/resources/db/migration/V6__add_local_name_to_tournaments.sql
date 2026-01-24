-- ============================================
-- V6: Add local_name column to tournaments table
-- ============================================
-- Stores the category/country name (e.g., "Inglaterra", "Brasil")

ALTER TABLE betting.tournaments ADD COLUMN local_name VARCHAR(255);

-- Index for filtering by category/country
CREATE INDEX idx_tournaments_local_name ON betting.tournaments(local_name);
