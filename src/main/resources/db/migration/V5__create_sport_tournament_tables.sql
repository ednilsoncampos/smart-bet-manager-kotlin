-- ============================================
-- V5: Create sport and tournament tables
-- ============================================

-- ============================================
-- 1. Create sport table
-- ============================================
CREATE TABLE betting.sports (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    external_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,

    CONSTRAINT fk_sport_provider
        FOREIGN KEY (provider_id)
        REFERENCES core.betting_providers(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_sport_provider_external
        UNIQUE (provider_id, external_id)
);

-- Index for faster lookups by provider
CREATE INDEX idx_sports_provider_id ON betting.sports(provider_id);

-- ============================================
-- 2. Create tournament table
-- ============================================
CREATE TABLE betting.tournaments (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    sport_id BIGINT NOT NULL,
    external_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,

    CONSTRAINT fk_tournament_provider
        FOREIGN KEY (provider_id)
        REFERENCES core.betting_providers(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tournament_sport
        FOREIGN KEY (sport_id)
        REFERENCES betting.sports(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_tournament_provider_external
        UNIQUE (provider_id, external_id)
);

-- Indexes for faster lookups
CREATE INDEX idx_tournaments_provider_id ON betting.tournaments(provider_id);
CREATE INDEX idx_tournaments_sport_id ON betting.tournaments(sport_id);
CREATE INDEX idx_tournaments_external_id ON betting.tournaments(external_id);
