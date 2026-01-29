-- ============================================
-- V8: Add performance indexes for critical queries
-- ============================================

-- ===== ÍNDICES CRÍTICOS (P0) =====

-- Índice para listagem de tickets por usuário (usado em TODAS as queries de tickets)
CREATE INDEX IF NOT EXISTS idx_bet_tickets_user_id
    ON betting.bet_tickets(user_id);

-- Índice composto para refresh de tickets abertos por usuário
-- Usado por: TicketService.refreshOpenTickets()
CREATE INDEX IF NOT EXISTS idx_bet_tickets_user_status
    ON betting.bet_tickets(user_id, ticket_status);

-- Índice único composto para verificação de duplicatas
-- Usado por: TicketService.importFromUrl() para evitar reimportar o mesmo bilhete
CREATE UNIQUE INDEX IF NOT EXISTS idx_bet_tickets_external_provider
    ON betting.bet_tickets(external_ticket_id, provider_id)
    WHERE external_ticket_id IS NOT NULL;

-- Índice para buscar seleções de um ticket (JOIN frequente)
CREATE INDEX IF NOT EXISTS idx_bet_selections_ticket_id
    ON betting.bet_selections(ticket_id);

-- ===== ÍNDICES DE PERFORMANCE (P1) =====

-- Índice composto para queries de analytics por usuário e status financeiro
-- Usado por: AnalyticsService.getPerformanceByProvider(), getPerformanceByMarket()
CREATE INDEX IF NOT EXISTS idx_bet_tickets_user_financial_status
    ON betting.bet_tickets(user_id, financial_status);

-- Índice composto para analytics por provider
-- Usado por: AnalyticsService.getPerformanceByProvider()
CREATE INDEX IF NOT EXISTS idx_bet_tickets_provider_status
    ON betting.bet_tickets(provider_id, ticket_status);

-- Índice para buscas por nome do evento (usado no Bet Builder)
-- Usado por: AnalyticsService.getPerformanceByMarket() quando agrupa por eventName
CREATE INDEX IF NOT EXISTS idx_bet_selections_event_name
    ON betting.bet_selections(event_name);

-- Índice composto para queries de seleções por ticket e mercado
-- Usado por: AnalyticsService.getPerformanceByMarket()
CREATE INDEX IF NOT EXISTS idx_bet_selections_ticket_market
    ON betting.bet_selections(ticket_id, market_type);

-- Índice composto para componentes de Bet Builder
-- Usado por: TicketService ao buscar componentes de seleções
CREATE INDEX IF NOT EXISTS idx_components_selection_status
    ON betting.bet_selection_components(selection_id, status);
