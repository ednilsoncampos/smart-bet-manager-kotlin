-- Smart Bet Manager - Initial Schema with password_hash
-- Version: 1.0.0

-- ============================================
-- Users Table
-- ============================================
CREATE TABLE users (
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

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_external_id ON users(external_id);

-- ============================================
-- Betting Providers Table
-- ============================================
CREATE TABLE betting_providers (
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

CREATE INDEX idx_betting_providers_slug ON betting_providers(slug);

-- Insert default providers
INSERT INTO betting_providers (slug, name, is_active, api_url_template, website_url) VALUES
    ('superbet', 'Superbet', TRUE, 'https://prod-superbet-betting.freetls.fastly.net/tickets/presentation-api/v3/SB_BR/ticket/{CODE}', 'https://superbet.bet.br'),
    ('betano', 'Betano', TRUE, 'https://www.betano.bet.br/api/betslip/v3/getBetslipById/{CODE}', 'https://www.betano.bet.br');

-- ============================================
-- Bankrolls Table
-- ============================================
CREATE TABLE bankrolls (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id BIGINT REFERENCES betting_providers(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    current_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_deposited DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_withdrawn DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_staked DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_returns DECIMAL(15, 2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000))
);

CREATE INDEX idx_bankrolls_user_id ON bankrolls(user_id);
CREATE INDEX idx_bankrolls_provider_id ON bankrolls(provider_id);

-- ============================================
-- Bet Tickets Table
-- ============================================
CREATE TABLE bet_tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id BIGINT NOT NULL REFERENCES betting_providers(id) ON DELETE RESTRICT,
    bankroll_id BIGINT REFERENCES bankrolls(id) ON DELETE SET NULL,
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
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000))
);

CREATE INDEX idx_bet_tickets_user_id ON bet_tickets(user_id);
CREATE INDEX idx_bet_tickets_provider_id ON bet_tickets(provider_id);
CREATE INDEX idx_bet_tickets_bankroll_id ON bet_tickets(bankroll_id);
CREATE INDEX idx_bet_tickets_external_id ON bet_tickets(external_ticket_id);
CREATE INDEX idx_bet_tickets_status ON bet_tickets(ticket_status);
CREATE INDEX idx_bet_tickets_financial_status ON bet_tickets(financial_status);
CREATE INDEX idx_bet_tickets_placed_at ON bet_tickets(placed_at);

-- ============================================
-- Bet Selections Table
-- ============================================
CREATE TABLE bet_selections (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES bet_tickets(id) ON DELETE CASCADE,
    external_selection_id VARCHAR(100),
    event_name VARCHAR(255) NOT NULL,
    tournament_name VARCHAR(255),
    market_type VARCHAR(100),
    selection VARCHAR(255) NOT NULL,
    odd DECIMAL(10, 4) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    event_date BIGINT,
    event_result VARCHAR(100),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    updated_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000))
);

CREATE INDEX idx_bet_selections_ticket_id ON bet_selections(ticket_id);
CREATE INDEX idx_bet_selections_status ON bet_selections(status);
CREATE INDEX idx_bet_selections_tournament ON bet_selections(tournament_name);
CREATE INDEX idx_bet_selections_market ON bet_selections(market_type);

-- ============================================
-- Bankroll Transactions Table
-- ============================================
CREATE TABLE bankroll_transactions (
    id BIGSERIAL PRIMARY KEY,
    bankroll_id BIGINT NOT NULL REFERENCES bankrolls(id) ON DELETE CASCADE,
    ticket_id BIGINT REFERENCES bet_tickets(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000))
);

CREATE INDEX idx_bankroll_transactions_bankroll_id ON bankroll_transactions(bankroll_id);
CREATE INDEX idx_bankroll_transactions_ticket_id ON bankroll_transactions(ticket_id);
CREATE INDEX idx_bankroll_transactions_type ON bankroll_transactions(type);
CREATE INDEX idx_bankroll_transactions_created_at ON bankroll_transactions(created_at);

-- ============================================
-- Provider API Requests Table
-- ============================================
CREATE TABLE provider_api_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    url VARCHAR(500) NOT NULL,
    provider_name VARCHAR(100),
    request_count INT NOT NULL DEFAULT 1,
    last_requested_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000)),
    created_at BIGINT NOT NULL DEFAULT (floor(EXTRACT(EPOCH FROM NOW()) * 1000))
);

CREATE INDEX idx_provider_api_requests_provider_name ON provider_api_requests(provider_name);
CREATE INDEX idx_provider_api_requests_request_count ON provider_api_requests(request_count DESC);
