-- ============================================
-- V4: Add cashout field, bet_selection_components table, and duplicate constraint
-- ============================================

-- ============================================
-- 1. Add is_cashed_out field to bet_tickets
-- ============================================
ALTER TABLE betting.bet_tickets ADD COLUMN is_cashed_out BOOLEAN DEFAULT FALSE;

-- ============================================
-- 2. Create bet_selection_components table for Bet Builder
-- ============================================
CREATE TABLE betting.bet_selection_components (
    id BIGSERIAL PRIMARY KEY,
    selection_id BIGINT NOT NULL,
    market_id VARCHAR(50),
    market_name VARCHAR(255) NOT NULL,
    selection_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at BIGINT NOT NULL,
    
    CONSTRAINT fk_component_selection 
        FOREIGN KEY (selection_id) 
        REFERENCES betting.bet_selections(id) 
        ON DELETE CASCADE
);

-- Index for faster lookups by selection
CREATE INDEX idx_components_selection_id ON betting.bet_selection_components(selection_id);

-- ============================================
-- 3. Add unique constraint to prevent duplicate tickets
-- ============================================
ALTER TABLE betting.bet_tickets 
    ADD CONSTRAINT uk_ticket_user_external 
    UNIQUE (user_id, external_ticket_id);
