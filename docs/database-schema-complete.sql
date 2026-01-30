-- ============================================
-- Smart Bet Manager - Complete Database Schema
-- ============================================
-- Versão: V8 (Final após todas as migrations)
-- SGBD: PostgreSQL 16+
-- Data: 2026-01-29
--
-- NOTA: Este arquivo representa o estado FINAL do banco após aplicar
--       todas as migrations V1 até V8. Use para referência ou para
--       criar ambiente de desenvolvimento do zero.
-- ============================================

-- ============================================
-- 1. CREATE SCHEMAS
-- ============================================
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS betting;
CREATE SCHEMA IF NOT EXISTS log;

-- ============================================
-- 2. CORE SCHEMA - Tabelas Fundamentais
-- ============================================

-- -------------------------------------------
-- 2.1 core.users
-- -------------------------------------------
CREATE TABLE core.users (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    password_hash VARCHAR(255),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000))
);

CREATE INDEX idx_users_email ON core.users(email);
CREATE INDEX idx_users_external_id ON core.users(external_id);

COMMENT ON TABLE core.users IS 'Usuários do sistema';
COMMENT ON COLUMN core.users.external_id IS 'ID de autenticação externa (OAuth)';
COMMENT ON COLUMN core.users.password_hash IS 'Hash bcrypt da senha';

-- -------------------------------------------
-- 2.2 core.betting_providers
-- -------------------------------------------
CREATE TABLE core.betting_providers (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    api_url_template VARCHAR(500),
    website_url VARCHAR(255),
    logo_url VARCHAR(500),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000))
);

CREATE INDEX idx_betting_providers_slug ON core.betting_providers(slug);

COMMENT ON TABLE core.betting_providers IS 'Casas de apostas suportadas';
COMMENT ON COLUMN core.betting_providers.api_url_template IS 'Template da URL da API (use {CODE} como placeholder)';

-- Dados iniciais de providers
INSERT INTO core.betting_providers (slug, name, is_active, api_url_template, website_url) VALUES
    ('superbet', 'Superbet', TRUE,
     'https://prod-superbet-betting.freetls.fastly.net/tickets/presentation-api/v3/SB_BR/ticket/{CODE}',
     'https://superbet.bet.br'),
    ('betano', 'Betano', TRUE,
     'https://www.betano.bet.br/api/betslip/v3/getBetslipById/{CODE}',
     'https://www.betano.bet.br');

-- -------------------------------------------
-- 2.3 core.bankrolls
-- -------------------------------------------
CREATE TABLE core.bankrolls (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id BIGINT,
    name VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    current_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_deposited DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_withdrawn DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_staked DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_returns DECIMAL(15, 2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_bankroll_user FOREIGN KEY (user_id)
        REFERENCES core.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bankroll_provider FOREIGN KEY (provider_id)
        REFERENCES core.betting_providers(id) ON DELETE SET NULL
);

CREATE INDEX idx_bankrolls_user_id ON core.bankrolls(user_id);
CREATE INDEX idx_bankrolls_provider_id ON core.bankrolls(provider_id);

COMMENT ON TABLE core.bankrolls IS 'Carteiras/bankrolls dos usuários';
COMMENT ON COLUMN core.bankrolls.current_balance IS 'Saldo atual calculado';

-- -------------------------------------------
-- 2.4 core.bankroll_transactions
-- -------------------------------------------
CREATE TABLE core.bankroll_transactions (
    id BIGSERIAL PRIMARY KEY,
    bankroll_id BIGINT NOT NULL,
    ticket_id BIGINT,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_transaction_bankroll FOREIGN KEY (bankroll_id)
        REFERENCES core.bankrolls(id) ON DELETE CASCADE
    -- FK para ticket será adicionada após criação da tabela betting.bet_tickets
);

CREATE INDEX idx_bankroll_transactions_bankroll_id ON core.bankroll_transactions(bankroll_id);
CREATE INDEX idx_bankroll_transactions_ticket_id ON core.bankroll_transactions(ticket_id);
CREATE INDEX idx_bankroll_transactions_type ON core.bankroll_transactions(type);
CREATE INDEX idx_bankroll_transactions_created_at ON core.bankroll_transactions(created_at);

COMMENT ON TABLE core.bankroll_transactions IS 'Histórico de transações financeiras';
COMMENT ON COLUMN core.bankroll_transactions.type IS 'DEPOSIT, WITHDRAW, BET, WIN, LOSS';

-- ============================================
-- 3. BETTING SCHEMA - Dados de Apostas
-- ============================================

-- -------------------------------------------
-- 3.1 betting.sports
-- -------------------------------------------
CREATE TABLE betting.sports (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    external_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_sport_provider FOREIGN KEY (provider_id)
        REFERENCES core.betting_providers(id) ON DELETE CASCADE,
    CONSTRAINT uk_sport_provider_external UNIQUE (provider_id, external_id)
);

CREATE INDEX idx_sports_provider_id ON betting.sports(provider_id);

COMMENT ON TABLE betting.sports IS 'Esportes disponíveis (futebol, basquete, etc)';

-- -------------------------------------------
-- 3.2 betting.tournaments
-- -------------------------------------------
CREATE TABLE betting.tournaments (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    sport_id BIGINT NOT NULL,
    external_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    local_name VARCHAR(255),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_tournament_provider FOREIGN KEY (provider_id)
        REFERENCES core.betting_providers(id) ON DELETE CASCADE,
    CONSTRAINT fk_tournament_sport FOREIGN KEY (sport_id)
        REFERENCES betting.sports(id) ON DELETE CASCADE,
    CONSTRAINT uk_tournament_provider_external UNIQUE (provider_id, external_id)
);

CREATE INDEX idx_tournaments_provider_id ON betting.tournaments(provider_id);
CREATE INDEX idx_tournaments_sport_id ON betting.tournaments(sport_id);
CREATE INDEX idx_tournaments_external_id ON betting.tournaments(external_id);
CREATE INDEX idx_tournaments_local_name ON betting.tournaments(local_name);

COMMENT ON TABLE betting.tournaments IS 'Torneios/competições (Premier League, etc)';
COMMENT ON COLUMN betting.tournaments.local_name IS 'Nome da categoria/país (ex: Inglaterra, Brasil)';

-- -------------------------------------------
-- 3.3 betting.bet_tickets
-- -------------------------------------------
CREATE TABLE betting.bet_tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    bankroll_id BIGINT,
    external_ticket_id VARCHAR(100),
    source_url VARCHAR(500),
    bet_type VARCHAR(50) NOT NULL DEFAULT 'SINGLE',
    bet_side VARCHAR(50) NOT NULL DEFAULT 'BACK',
    stake DECIMAL(15, 2) NOT NULL,
    total_odd DECIMAL(10, 4) NOT NULL,
    potential_payout DECIMAL(15, 2),
    actual_payout DECIMAL(15, 2),
    ticket_status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    financial_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    profit_loss DECIMAL(15, 2) NOT NULL DEFAULT 0,
    roi DECIMAL(10, 4) NOT NULL DEFAULT 0,
    system_description VARCHAR(50),
    placed_at BIGINT,
    settled_at BIGINT,
    is_cashed_out BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id)
        REFERENCES core.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_provider FOREIGN KEY (provider_id)
        REFERENCES core.betting_providers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_bankroll FOREIGN KEY (bankroll_id)
        REFERENCES core.bankrolls(id) ON DELETE SET NULL,
    CONSTRAINT uk_ticket_user_external UNIQUE (user_id, external_ticket_id)
);

-- Índices originais (V1)
CREATE INDEX idx_bet_tickets_user_id ON betting.bet_tickets(user_id);
CREATE INDEX idx_bet_tickets_provider_id ON betting.bet_tickets(provider_id);
CREATE INDEX idx_bet_tickets_bankroll_id ON betting.bet_tickets(bankroll_id);
CREATE INDEX idx_bet_tickets_external_id ON betting.bet_tickets(external_ticket_id);
CREATE INDEX idx_bet_tickets_status ON betting.bet_tickets(ticket_status);
CREATE INDEX idx_bet_tickets_financial_status ON betting.bet_tickets(financial_status);
CREATE INDEX idx_bet_tickets_placed_at ON betting.bet_tickets(placed_at);

-- Índices de performance (V8)
CREATE INDEX idx_bet_tickets_user_status ON betting.bet_tickets(user_id, ticket_status);
CREATE UNIQUE INDEX idx_bet_tickets_external_provider
    ON betting.bet_tickets(external_ticket_id, provider_id)
    WHERE external_ticket_id IS NOT NULL;
CREATE INDEX idx_bet_tickets_user_financial_status ON betting.bet_tickets(user_id, financial_status);
CREATE INDEX idx_bet_tickets_provider_status ON betting.bet_tickets(provider_id, ticket_status);

COMMENT ON TABLE betting.bet_tickets IS 'Bilhetes de apostas';
COMMENT ON COLUMN betting.bet_tickets.bet_type IS 'SINGLE, MULTIPLE, SYSTEM';
COMMENT ON COLUMN betting.bet_tickets.ticket_status IS 'OPEN, WON, LOST, VOID, CASHED_OUT';
COMMENT ON COLUMN betting.bet_tickets.financial_status IS 'PENDING, FULL_WIN, PARTIAL_WIN, BREAK_EVEN, PARTIAL_LOSS, TOTAL_LOSS';

-- -------------------------------------------
-- 3.4 betting.bet_selections
-- -------------------------------------------
CREATE TABLE betting.bet_selections (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    tournament_id BIGINT,
    external_selection_id VARCHAR(100),
    event_name VARCHAR(255) NOT NULL,
    market_type VARCHAR(100),
    selection VARCHAR(255) NOT NULL,
    odd DECIMAL(10, 4) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    event_date BIGINT,
    event_result VARCHAR(100),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_selection_ticket FOREIGN KEY (ticket_id)
        REFERENCES betting.bet_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_selection_tournament FOREIGN KEY (tournament_id)
        REFERENCES betting.tournaments(id) ON DELETE SET NULL
);

CREATE INDEX idx_bet_selections_ticket_id ON betting.bet_selections(ticket_id);
CREATE INDEX idx_bet_selections_status ON betting.bet_selections(status);
CREATE INDEX idx_bet_selections_market ON betting.bet_selections(market_type);
CREATE INDEX idx_bet_selections_tournament_id ON betting.bet_selections(tournament_id);

-- Índices de performance (V8)
CREATE INDEX idx_bet_selections_event_name ON betting.bet_selections(event_name);
CREATE INDEX idx_bet_selections_ticket_market ON betting.bet_selections(ticket_id, market_type);

COMMENT ON TABLE betting.bet_selections IS 'Seleções individuais (pernas) de cada aposta';
COMMENT ON COLUMN betting.bet_selections.status IS 'PENDING, WON, LOST, VOID, HALF_WON, HALF_LOST';

-- -------------------------------------------
-- 3.5 betting.bet_selection_components
-- -------------------------------------------
CREATE TABLE betting.bet_selection_components (
    id BIGSERIAL PRIMARY KEY,
    selection_id BIGINT NOT NULL,
    market_id VARCHAR(50),
    market_name VARCHAR(255) NOT NULL,
    selection_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_component_selection FOREIGN KEY (selection_id)
        REFERENCES betting.bet_selections(id) ON DELETE CASCADE
);

CREATE INDEX idx_components_selection_id ON betting.bet_selection_components(selection_id);

-- Índice de performance (V8)
CREATE INDEX idx_components_selection_status
    ON betting.bet_selection_components(selection_id, status);

COMMENT ON TABLE betting.bet_selection_components IS 'Componentes de Bet Builder (múltiplos mercados no mesmo evento)';

-- ============================================
-- 4. PUBLIC SCHEMA - Logs e Auditoria
-- ============================================

-- -------------------------------------------
-- 4.1 public.provider_api_requests
-- -------------------------------------------
CREATE TABLE public.provider_api_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    url VARCHAR(500) NOT NULL,
    provider_name VARCHAR(100),
    request_count INT NOT NULL DEFAULT 1,
    last_requested_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_api_request_user FOREIGN KEY (user_id)
        REFERENCES core.users(id) ON DELETE SET NULL
);

CREATE INDEX idx_provider_api_requests_provider_name ON public.provider_api_requests(provider_name);
CREATE INDEX idx_provider_api_requests_request_count ON public.provider_api_requests(request_count DESC);

COMMENT ON TABLE public.provider_api_requests IS 'Log de requisições às APIs dos providers';

-- ============================================
-- 5. ADICIONAR FK FALTANTE
-- ============================================

-- FK de bankroll_transactions para bet_tickets (criada após a tabela existir)
ALTER TABLE core.bankroll_transactions
    ADD CONSTRAINT fk_transaction_ticket FOREIGN KEY (ticket_id)
    REFERENCES betting.bet_tickets(id) ON DELETE SET NULL;

-- ============================================
-- 6. VIEWS ÚTEIS (Opcional)
-- ============================================

-- View para estatísticas gerais de usuários
CREATE OR REPLACE VIEW core.user_stats AS
SELECT
    u.id AS user_id,
    u.name,
    u.email,
    COUNT(DISTINCT t.id) AS total_tickets,
    COUNT(DISTINCT CASE WHEN t.ticket_status = 'OPEN' THEN t.id END) AS open_tickets,
    COUNT(DISTINCT CASE WHEN t.ticket_status = 'WON' THEN t.id END) AS won_tickets,
    COUNT(DISTINCT CASE WHEN t.ticket_status = 'LOST' THEN t.id END) AS lost_tickets,
    COALESCE(SUM(t.stake), 0) AS total_staked,
    COALESCE(SUM(t.actual_payout), 0) AS total_returns,
    COALESCE(SUM(t.profit_loss), 0) AS total_profit_loss
FROM core.users u
LEFT JOIN betting.bet_tickets t ON u.id = t.user_id
GROUP BY u.id, u.name, u.email;

COMMENT ON VIEW core.user_stats IS 'Estatísticas agregadas por usuário';

-- ============================================
-- FIM DO SCHEMA COMPLETO
-- ============================================

-- Para verificar todas as tabelas:
-- SELECT schemaname, tablename FROM pg_tables
-- WHERE schemaname IN ('core', 'betting', 'public')
-- ORDER BY schemaname, tablename;

-- Para verificar todos os índices:
-- SELECT schemaname, tablename, indexname
-- FROM pg_indexes
-- WHERE schemaname IN ('core', 'betting')
-- ORDER BY schemaname, tablename, indexname;

-- Para verificar todas as FKs:
-- SELECT
--     tc.table_schema,
--     tc.table_name,
--     kcu.column_name,
--     ccu.table_schema AS foreign_table_schema,
--     ccu.table_name AS foreign_table_name,
--     ccu.column_name AS foreign_column_name
-- FROM information_schema.table_constraints AS tc
-- JOIN information_schema.key_column_usage AS kcu
--     ON tc.constraint_name = kcu.constraint_name
--     AND tc.table_schema = kcu.table_schema
-- JOIN information_schema.constraint_column_usage AS ccu
--     ON ccu.constraint_name = tc.constraint_name
-- WHERE tc.constraint_type = 'FOREIGN KEY'
--     AND tc.table_schema IN ('core', 'betting')
-- ORDER BY tc.table_schema, tc.table_name;
