-- ============================================
-- Smart Bet Manager - Complete Database Schema
-- ============================================
-- Version: 1.0.0 (Consolidated)
-- Date: 2026-01-29
-- Description: Schema completo com todas as tabelas (core, betting, analytics)
--              Consolida todas as migrations anteriores em um único arquivo
-- ============================================

-- ============================================
-- SECTION 1: CREATE SCHEMAS
-- ============================================

CREATE SCHEMA IF NOT EXISTS core;
COMMENT ON SCHEMA core IS 'Dados principais: usuários, providers, bankrolls';

CREATE SCHEMA IF NOT EXISTS betting;
COMMENT ON SCHEMA betting IS 'Dados de apostas: tickets, seleções, torneios';

CREATE SCHEMA IF NOT EXISTS analytics;
COMMENT ON SCHEMA analytics IS 'Métricas pré-agregadas para performance (event-driven)';

-- ============================================
-- SECTION 2: CORE SCHEMA - Tabelas Fundamentais
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
COMMENT ON COLUMN core.users.external_id IS 'ID de autenticação externa (OAuth, etc)';
COMMENT ON COLUMN core.users.password_hash IS 'Hash bcrypt da senha';
COMMENT ON COLUMN core.users.role IS 'USER, ADMIN';
COMMENT ON COLUMN core.users.created_at IS 'Timestamp em milissegundos (Unix epoch * 1000)';

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

COMMENT ON TABLE core.bankrolls IS 'Carteiras/bankrolls dos usuários por casa de aposta';
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
);

CREATE INDEX idx_bankroll_transactions_bankroll_id ON core.bankroll_transactions(bankroll_id);
CREATE INDEX idx_bankroll_transactions_ticket_id ON core.bankroll_transactions(ticket_id);
CREATE INDEX idx_bankroll_transactions_type ON core.bankroll_transactions(type);
CREATE INDEX idx_bankroll_transactions_created_at ON core.bankroll_transactions(created_at);

COMMENT ON TABLE core.bankroll_transactions IS 'Histórico de transações financeiras';
COMMENT ON COLUMN core.bankroll_transactions.type IS 'DEPOSIT, WITHDRAW, BET, WIN, LOSS';

-- ============================================
-- SECTION 3: BETTING SCHEMA - Dados de Apostas
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

COMMENT ON TABLE betting.tournaments IS 'Torneios/competições (Premier League, Champions League, etc)';
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

-- Índices básicos
CREATE INDEX idx_bet_tickets_user_id ON betting.bet_tickets(user_id);
CREATE INDEX idx_bet_tickets_provider_id ON betting.bet_tickets(provider_id);
CREATE INDEX idx_bet_tickets_bankroll_id ON betting.bet_tickets(bankroll_id);
CREATE INDEX idx_bet_tickets_external_id ON betting.bet_tickets(external_ticket_id);
CREATE INDEX idx_bet_tickets_status ON betting.bet_tickets(ticket_status);
CREATE INDEX idx_bet_tickets_financial_status ON betting.bet_tickets(financial_status);
CREATE INDEX idx_bet_tickets_placed_at ON betting.bet_tickets(placed_at);

-- Índices de performance (críticos para queries frequentes)
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
    sport_id VARCHAR(50),
    is_bet_builder BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_selection_ticket FOREIGN KEY (ticket_id)
        REFERENCES betting.bet_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_selection_tournament FOREIGN KEY (tournament_id)
        REFERENCES betting.tournaments(id) ON DELETE SET NULL
);

-- Índices básicos
CREATE INDEX idx_bet_selections_ticket_id ON betting.bet_selections(ticket_id);
CREATE INDEX idx_bet_selections_status ON betting.bet_selections(status);
CREATE INDEX idx_bet_selections_market ON betting.bet_selections(market_type);
CREATE INDEX idx_bet_selections_tournament_id ON betting.bet_selections(tournament_id);

-- Índices de performance
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
CREATE INDEX idx_components_selection_status ON betting.bet_selection_components(selection_id, status);

COMMENT ON TABLE betting.bet_selection_components IS 'Componentes de Bet Builder (múltiplos mercados no mesmo evento)';

-- ============================================
-- SECTION 4: ANALYTICS SCHEMA - Métricas Event-Driven
-- ============================================

-- -------------------------------------------
-- 4.1 analytics.performance_overall
-- -------------------------------------------
CREATE TABLE analytics.performance_overall (
    user_id BIGINT PRIMARY KEY,

    -- Contadores de tickets
    total_tickets INT NOT NULL DEFAULT 0,
    tickets_won INT NOT NULL DEFAULT 0,
    tickets_lost INT NOT NULL DEFAULT 0,
    tickets_void INT NOT NULL DEFAULT 0,
    tickets_cashed_out INT NOT NULL DEFAULT 0,

    -- Contadores granulares por FinancialStatus
    tickets_full_won INT NOT NULL DEFAULT 0,
    tickets_partial_won INT NOT NULL DEFAULT 0,
    tickets_break_even INT NOT NULL DEFAULT 0,
    tickets_partial_lost INT NOT NULL DEFAULT 0,
    tickets_total_lost INT NOT NULL DEFAULT 0,

    -- Métricas financeiras
    total_stake DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_return DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_profit DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Métricas calculadas
    roi DECIMAL(10,4) NOT NULL DEFAULT 0,
    win_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    success_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    avg_odd DECIMAL(10,4) DEFAULT NULL,
    avg_stake DECIMAL(15,2) DEFAULT NULL,

    -- Gamificação (streaks)
    current_streak INT DEFAULT 0,
    best_win_streak INT DEFAULT 0,
    worst_loss_streak INT DEFAULT 0,

    -- Records pessoais
    biggest_win DECIMAL(15,2) DEFAULT NULL,
    biggest_loss DECIMAL(15,2) DEFAULT NULL,
    best_roi_ticket DECIMAL(10,4) DEFAULT NULL,

    -- Timestamps
    first_bet_at BIGINT,
    last_settled_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    CONSTRAINT fk_performance_overall_user
        FOREIGN KEY (user_id) REFERENCES core.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_performance_overall_roi ON analytics.performance_overall(roi DESC);
CREATE INDEX idx_performance_overall_total_profit ON analytics.performance_overall(total_profit DESC);
CREATE INDEX idx_performance_overall_win_rate ON analytics.performance_overall(win_rate DESC);
CREATE INDEX idx_performance_overall_last_settled ON analytics.performance_overall(last_settled_at DESC);

COMMENT ON TABLE analytics.performance_overall IS 'Performance geral agregada por usuário (all-time)';
COMMENT ON COLUMN analytics.performance_overall.current_streak IS 'Sequência atual: >0 = vitórias seguidas, <0 = derrotas seguidas';
COMMENT ON COLUMN analytics.performance_overall.roi IS 'ROI em porcentagem (total_profit / total_stake * 100)';
COMMENT ON COLUMN analytics.performance_overall.win_rate IS 'Taxa de acerto pura - apenas FULL_WIN (todas seleções corretas) em %';
COMMENT ON COLUMN analytics.performance_overall.success_rate IS 'Taxa de sucesso - FULL_WIN + PARTIAL_WIN (ganhos totais + parciais incluindo sistemas e cashouts) em %';
COMMENT ON COLUMN analytics.performance_overall.tickets_full_won IS 'Vitórias completas (FULL_WIN): retorno >= potencial máximo';
COMMENT ON COLUMN analytics.performance_overall.tickets_partial_won IS 'Vitórias parciais (PARTIAL_WIN): via sistema parcial ou cashout com lucro';
COMMENT ON COLUMN analytics.performance_overall.tickets_break_even IS 'Empates (BREAK_EVEN): retorno = stake (anulação, cashout exato, ou sistema)';
COMMENT ON COLUMN analytics.performance_overall.tickets_partial_lost IS 'Perdas parciais (PARTIAL_LOSS): comum em sistemas, ou cashout com prejuízo';
COMMENT ON COLUMN analytics.performance_overall.tickets_total_lost IS 'Perdas totais (TOTAL_LOSS): retorno = 0';
COMMENT ON COLUMN analytics.performance_overall.tickets_won IS 'Total de vitórias (full_won + partial_won) - compatibilidade';
COMMENT ON COLUMN analytics.performance_overall.tickets_lost IS 'Total de derrotas (partial_lost + total_lost) - compatibilidade';

-- -------------------------------------------
-- 4.2 analytics.performance_by_month
-- -------------------------------------------
CREATE TABLE analytics.performance_by_month (
    user_id BIGINT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,

    -- Contadores de tickets
    total_tickets INT NOT NULL DEFAULT 0,
    tickets_won INT NOT NULL DEFAULT 0,
    tickets_lost INT NOT NULL DEFAULT 0,
    tickets_void INT NOT NULL DEFAULT 0,

    -- Contadores granulares por FinancialStatus
    tickets_full_won INT NOT NULL DEFAULT 0,
    tickets_partial_won INT NOT NULL DEFAULT 0,
    tickets_break_even INT NOT NULL DEFAULT 0,
    tickets_partial_lost INT NOT NULL DEFAULT 0,
    tickets_total_lost INT NOT NULL DEFAULT 0,

    -- Métricas financeiras
    total_stake DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_profit DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Métricas calculadas
    roi DECIMAL(10,4) NOT NULL DEFAULT 0,
    win_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    success_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    avg_stake DECIMAL(15,2) DEFAULT NULL,
    avg_odd DECIMAL(10,4) DEFAULT NULL,

    -- Timestamps
    first_bet_at BIGINT,
    last_settled_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    PRIMARY KEY (user_id, year, month),

    CONSTRAINT fk_performance_month_user
        FOREIGN KEY (user_id) REFERENCES core.users(id) ON DELETE CASCADE,
    CONSTRAINT chk_month_range CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_year_range CHECK (year BETWEEN 2020 AND 2100)
);

CREATE INDEX idx_performance_month_user_period ON analytics.performance_by_month(user_id, year DESC, month DESC);
CREATE INDEX idx_performance_month_period_profit ON analytics.performance_by_month(year, month, total_profit DESC);
CREATE INDEX idx_performance_month_roi ON analytics.performance_by_month(user_id, roi DESC);

COMMENT ON TABLE analytics.performance_by_month IS 'Performance agregada por usuário e mês (permite consultas por período)';
COMMENT ON COLUMN analytics.performance_by_month.month IS 'Mês do ano (1-12)';
COMMENT ON COLUMN analytics.performance_by_month.win_rate IS 'Taxa de acerto pura - apenas FULL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_month.success_rate IS 'Taxa de sucesso - FULL_WIN + PARTIAL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_month.tickets_full_won IS 'Vitórias completas no mês';
COMMENT ON COLUMN analytics.performance_by_month.tickets_partial_won IS 'Vitórias parciais no mês (sistema ou cashout)';
COMMENT ON COLUMN analytics.performance_by_month.tickets_break_even IS 'Empates no mês';
COMMENT ON COLUMN analytics.performance_by_month.tickets_partial_lost IS 'Perdas parciais no mês';
COMMENT ON COLUMN analytics.performance_by_month.tickets_total_lost IS 'Perdas totais no mês';

-- -------------------------------------------
-- 4.3 analytics.performance_by_provider
-- -------------------------------------------
CREATE TABLE analytics.performance_by_provider (
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,

    -- Contadores
    total_tickets INT NOT NULL DEFAULT 0,
    tickets_won INT NOT NULL DEFAULT 0,
    tickets_lost INT NOT NULL DEFAULT 0,
    tickets_void INT NOT NULL DEFAULT 0,
    tickets_cashed_out INT NOT NULL DEFAULT 0,

    -- Contadores granulares por FinancialStatus
    tickets_full_won INT NOT NULL DEFAULT 0,
    tickets_partial_won INT NOT NULL DEFAULT 0,
    tickets_break_even INT NOT NULL DEFAULT 0,
    tickets_partial_lost INT NOT NULL DEFAULT 0,
    tickets_total_lost INT NOT NULL DEFAULT 0,

    -- Métricas financeiras
    total_stake DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_profit DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Métricas calculadas
    roi DECIMAL(10,4) NOT NULL DEFAULT 0,
    win_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    success_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    avg_odd DECIMAL(10,4) DEFAULT NULL,
    avg_stake DECIMAL(10,2) DEFAULT NULL,

    -- Timestamps
    first_bet_at BIGINT,
    last_settled_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    PRIMARY KEY (user_id, provider_id),

    CONSTRAINT fk_performance_provider_user
        FOREIGN KEY (user_id) REFERENCES core.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_performance_provider_provider
        FOREIGN KEY (provider_id) REFERENCES core.betting_providers(id) ON DELETE CASCADE
);

CREATE INDEX idx_performance_provider_user_roi ON analytics.performance_by_provider(user_id, roi DESC);
CREATE INDEX idx_performance_provider_comparison ON analytics.performance_by_provider(provider_id, roi DESC);
CREATE INDEX idx_performance_provider_profit ON analytics.performance_by_provider(total_profit DESC);

COMMENT ON TABLE analytics.performance_by_provider IS 'Performance agregada por usuário e casa de aposta';
COMMENT ON COLUMN analytics.performance_by_provider.win_rate IS 'Taxa de acerto pura - apenas FULL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_provider.success_rate IS 'Taxa de sucesso - FULL_WIN + PARTIAL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_provider.tickets_full_won IS 'Vitórias completas no provider';
COMMENT ON COLUMN analytics.performance_by_provider.tickets_partial_won IS 'Vitórias parciais no provider';
COMMENT ON COLUMN analytics.performance_by_provider.tickets_break_even IS 'Empates no provider';
COMMENT ON COLUMN analytics.performance_by_provider.tickets_partial_lost IS 'Perdas parciais no provider';
COMMENT ON COLUMN analytics.performance_by_provider.tickets_total_lost IS 'Perdas totais no provider';

-- -------------------------------------------
-- 4.4 analytics.performance_by_market
-- -------------------------------------------
CREATE TABLE analytics.performance_by_market (
    user_id BIGINT NOT NULL,
    market_type VARCHAR(100) NOT NULL,

    -- Contadores (baseados em seleções, não tickets)
    total_selections INT NOT NULL DEFAULT 0,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    voids INT NOT NULL DEFAULT 0,

    -- Tickets únicos que incluem esse mercado
    unique_tickets INT NOT NULL DEFAULT 0,

    -- Contadores granulares por FinancialStatus dos tickets
    tickets_full_won INT NOT NULL DEFAULT 0,
    tickets_partial_won INT NOT NULL DEFAULT 0,
    tickets_break_even INT NOT NULL DEFAULT 0,
    tickets_partial_lost INT NOT NULL DEFAULT 0,
    tickets_total_lost INT NOT NULL DEFAULT 0,

    -- Métricas financeiras
    total_stake DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_profit DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Métricas calculadas
    roi DECIMAL(10,4) NOT NULL DEFAULT 0,
    win_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    success_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    avg_odd DECIMAL(10,4) DEFAULT NULL,

    -- Timestamps
    first_bet_at BIGINT,
    last_settled_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    PRIMARY KEY (user_id, market_type),

    CONSTRAINT fk_performance_market_user
        FOREIGN KEY (user_id) REFERENCES core.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_performance_market_user_roi ON analytics.performance_by_market(user_id, roi DESC);
CREATE INDEX idx_performance_market_profit ON analytics.performance_by_market(total_profit DESC);
CREATE INDEX idx_performance_market_type_roi ON analytics.performance_by_market(market_type, roi DESC);

COMMENT ON TABLE analytics.performance_by_market IS 'Performance agregada por usuário e tipo de mercado';
COMMENT ON COLUMN analytics.performance_by_market.total_selections IS 'Total de seleções (uma aposta múltipla conta N vezes)';
COMMENT ON COLUMN analytics.performance_by_market.unique_tickets IS 'Número de tickets únicos que incluem esse mercado';
COMMENT ON COLUMN analytics.performance_by_market.win_rate IS 'Taxa de acerto pura - apenas FULL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_market.success_rate IS 'Taxa de sucesso - FULL_WIN + PARTIAL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_market.tickets_full_won IS 'Tickets com FULL_WIN que incluem este mercado';
COMMENT ON COLUMN analytics.performance_by_market.tickets_partial_won IS 'Tickets com PARTIAL_WIN que incluem este mercado';
COMMENT ON COLUMN analytics.performance_by_market.tickets_break_even IS 'Tickets com BREAK_EVEN que incluem este mercado';
COMMENT ON COLUMN analytics.performance_by_market.tickets_partial_lost IS 'Tickets com PARTIAL_LOSS que incluem este mercado';
COMMENT ON COLUMN analytics.performance_by_market.tickets_total_lost IS 'Tickets com TOTAL_LOSS que incluem este mercado';

-- -------------------------------------------
-- 4.5 analytics.performance_by_tournament
-- -------------------------------------------
CREATE TABLE analytics.performance_by_tournament (
    user_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,

    -- Contadores de tickets
    total_tickets INT NOT NULL DEFAULT 0,
    tickets_won INT NOT NULL DEFAULT 0,
    tickets_lost INT NOT NULL DEFAULT 0,
    tickets_void INT NOT NULL DEFAULT 0,

    -- Contadores granulares por FinancialStatus
    tickets_full_won INT NOT NULL DEFAULT 0,
    tickets_partial_won INT NOT NULL DEFAULT 0,
    tickets_break_even INT NOT NULL DEFAULT 0,
    tickets_partial_lost INT NOT NULL DEFAULT 0,
    tickets_total_lost INT NOT NULL DEFAULT 0,

    -- Métricas financeiras
    total_stake DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_profit DECIMAL(15,2) NOT NULL DEFAULT 0,

    -- Métricas calculadas
    roi DECIMAL(10,4) NOT NULL DEFAULT 0,
    win_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    success_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    avg_odd DECIMAL(10,4) DEFAULT NULL,

    -- Timestamps
    first_bet_at BIGINT,
    last_settled_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),

    PRIMARY KEY (user_id, tournament_id),

    CONSTRAINT fk_performance_tournament_user
        FOREIGN KEY (user_id) REFERENCES core.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_performance_tournament_tournament
        FOREIGN KEY (tournament_id) REFERENCES betting.tournaments(id) ON DELETE CASCADE
);

CREATE INDEX idx_performance_tournament_user_roi ON analytics.performance_by_tournament(user_id, roi DESC);
CREATE INDEX idx_performance_tournament_user_profit ON analytics.performance_by_tournament(user_id, total_profit DESC);
CREATE INDEX idx_performance_tournament_comparison ON analytics.performance_by_tournament(tournament_id, roi DESC);

COMMENT ON TABLE analytics.performance_by_tournament IS 'Performance agregada por usuário e torneio/campeonato';
COMMENT ON COLUMN analytics.performance_by_tournament.win_rate IS 'Taxa de acerto pura - apenas FULL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_tournament.success_rate IS 'Taxa de sucesso - FULL_WIN + PARTIAL_WIN em %';
COMMENT ON COLUMN analytics.performance_by_tournament.tickets_full_won IS 'Vitórias completas no torneio';
COMMENT ON COLUMN analytics.performance_by_tournament.tickets_partial_won IS 'Vitórias parciais no torneio';
COMMENT ON COLUMN analytics.performance_by_tournament.tickets_break_even IS 'Empates no torneio';
COMMENT ON COLUMN analytics.performance_by_tournament.tickets_partial_lost IS 'Perdas parciais no torneio';
COMMENT ON COLUMN analytics.performance_by_tournament.tickets_total_lost IS 'Perdas totais no torneio';

-- ============================================
-- SECTION 5: PUBLIC SCHEMA - Logs
-- ============================================

-- -------------------------------------------
-- 5.1 public.provider_api_requests
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

COMMENT ON TABLE public.provider_api_requests IS 'Log de requisições às APIs dos providers (auditoria)';

-- ============================================
-- SECTION 6: FOREIGN KEYS ADICIONAIS
-- ============================================

-- FK de bankroll_transactions para bet_tickets
ALTER TABLE core.bankroll_transactions
    ADD CONSTRAINT fk_transaction_ticket FOREIGN KEY (ticket_id)
    REFERENCES betting.bet_tickets(id) ON DELETE SET NULL;

-- ============================================
-- SECTION 7: VIEWS ÚTEIS
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

COMMENT ON VIEW core.user_stats IS 'Estatísticas agregadas por usuário (calculadas em tempo real)';

-- ============================================
-- FINAL NOTES
-- ============================================

-- Para verificar todas as tabelas criadas:
-- SELECT schemaname, tablename FROM pg_tables
-- WHERE schemaname IN ('core', 'betting', 'analytics')
-- ORDER BY schemaname, tablename;

-- Para verificar todos os índices:
-- SELECT schemaname, tablename, indexname
-- FROM pg_indexes
-- WHERE schemaname IN ('core', 'betting', 'analytics')
-- ORDER BY schemaname, tablename, indexname;

-- Total de tabelas criadas:
-- core: 4 tabelas (users, betting_providers, bankrolls, bankroll_transactions)
-- betting: 5 tabelas (sports, tournaments, bet_tickets, bet_selections, bet_selection_components)
-- analytics: 5 tabelas (performance_overall, performance_by_month, performance_by_provider, performance_by_market, performance_by_tournament)
-- public: 1 tabela (provider_api_requests)
-- TOTAL: 15 tabelas

-- Total de índices criados: 40+
-- Total de foreign keys: 18
-- Total de constraints únicos: 8

-- ============================================
-- END OF MIGRATION V1
-- ============================================
