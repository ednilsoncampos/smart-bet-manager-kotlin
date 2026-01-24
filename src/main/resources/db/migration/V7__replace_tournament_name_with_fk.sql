-- ============================================
-- V7: Replace tournament_name with tournament_id FK
-- ============================================

-- 1. Add tournament_id column (nullable for migration)
ALTER TABLE betting.bet_selections
    ADD COLUMN tournament_id BIGINT;

-- 2. Add FK constraint
ALTER TABLE betting.bet_selections
    ADD CONSTRAINT fk_selection_tournament
    FOREIGN KEY (tournament_id)
    REFERENCES betting.tournaments(id)
    ON DELETE SET NULL;

-- 3. Drop old index
DROP INDEX IF EXISTS betting.idx_bet_selections_tournament;

-- 4. Add new index
CREATE INDEX idx_bet_selections_tournament_id
    ON betting.bet_selections(tournament_id);

-- 5. Drop old column
ALTER TABLE betting.bet_selections
    DROP COLUMN tournament_name;
