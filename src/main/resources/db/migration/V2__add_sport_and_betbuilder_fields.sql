-- Smart Bet Manager - Add sport_id and is_bet_builder fields
-- Version: 2.0.0

-- ============================================
-- Add new columns to bet_selections
-- ============================================

-- sport_id: ID do esporte (FOOT, BASK, TENN, etc.)
-- Usado para agrupar análises por esporte
ALTER TABLE bet_selections ADD COLUMN sport_id VARCHAR(50);

-- is_bet_builder: Indica se é uma aposta combinada (Bet Builder)
-- Usado para segmentar apostas combinadas para análise de mercados lucrativos
ALTER TABLE bet_selections ADD COLUMN is_bet_builder BOOLEAN DEFAULT FALSE;

-- ============================================
-- Create indexes for new columns
-- ============================================
CREATE INDEX idx_bet_selections_sport_id ON bet_selections(sport_id);
CREATE INDEX idx_bet_selections_is_bet_builder ON bet_selections(is_bet_builder);
