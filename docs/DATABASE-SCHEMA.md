# Smart Bet Manager - Database Schema Documentation

**Versão:** V8 (após todas as migrations)
**SGBD:** PostgreSQL 16
**Data:** 2026-01-29

---

## 📊 Visão Geral

O banco de dados é organizado em **3 schemas**:

| Schema | Propósito | Tabelas |
|--------|-----------|---------|
| `core` | Dados principais (usuários, providers, bankrolls) | 4 tabelas |
| `betting` | Dados de apostas (tickets, seleções) | 5 tabelas |
| `public` | Logs e auditoria | 1 tabela |

**Total:** 10 tabelas | 9+ relacionamentos | 30+ índices

---

## 📋 Tabelas e Campos

### Schema: `core`

#### 1. `core.users`
Armazena usuários do sistema.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único do usuário |
| `external_id` | VARCHAR(255) | UNIQUE | ID externo (OAuth, etc) |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Email do usuário |
| `name` | VARCHAR(255) | NOT NULL | Nome completo |
| `avatar_url` | VARCHAR(500) | | URL do avatar |
| `role` | VARCHAR(50) | NOT NULL, DEFAULT 'USER' | Papel (USER, ADMIN) |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Status ativo |
| `password_hash` | VARCHAR(255) | | Hash da senha (bcrypt) |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |
| `updated_at` | BIGINT | NOT NULL | Timestamp de atualização (ms) |

**Índices:**
- `idx_users_email` ON (email)
- `idx_users_external_id` ON (external_id)

---

#### 2. `core.betting_providers`
Casas de apostas suportadas.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único do provider |
| `slug` | VARCHAR(50) | NOT NULL, UNIQUE | Identificador único (superbet, betano) |
| `name` | VARCHAR(100) | NOT NULL | Nome exibido |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Provider ativo |
| `api_url_template` | VARCHAR(500) | | Template da URL da API |
| `website_url` | VARCHAR(255) | | URL do site |
| `logo_url` | VARCHAR(500) | | URL do logo |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |
| `updated_at` | BIGINT | NOT NULL | Timestamp de atualização (ms) |

**Índices:**
- `idx_betting_providers_slug` ON (slug)

**Dados Iniciais:**
- Superbet (slug: 'superbet')
- Betano (slug: 'betano')

---

#### 3. `core.bankrolls`
Carteiras/bankrolls dos usuários por casa de aposta.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único do bankroll |
| `user_id` | BIGINT | NOT NULL, FK → users.id | Dono do bankroll |
| `provider_id` | BIGINT | FK → betting_providers.id | Casa de aposta |
| `name` | VARCHAR(100) | NOT NULL | Nome do bankroll |
| `currency` | VARCHAR(10) | NOT NULL, DEFAULT 'BRL' | Moeda |
| `current_balance` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Saldo atual |
| `total_deposited` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Total depositado |
| `total_withdrawn` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Total sacado |
| `total_staked` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Total apostado |
| `total_returns` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Total de retornos |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Bankroll ativo |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |
| `updated_at` | BIGINT | NOT NULL | Timestamp de atualização (ms) |

**Relacionamentos:**
- `user_id` → `core.users(id)` ON DELETE CASCADE
- `provider_id` → `core.betting_providers(id)` ON DELETE SET NULL

**Índices:**
- `idx_bankrolls_user_id` ON (user_id)
- `idx_bankrolls_provider_id` ON (provider_id)

---

#### 4. `core.bankroll_transactions`
Transações financeiras nos bankrolls.

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único da transação |
| `bankroll_id` | BIGINT | NOT NULL, FK → bankrolls.id | Bankroll relacionado |
| `ticket_id` | BIGINT | FK → bet_tickets.id | Ticket relacionado (opcional) |
| `type` | VARCHAR(50) | NOT NULL | Tipo (DEPOSIT, WITHDRAW, BET, WIN, LOSS) |
| `amount` | DECIMAL(15,2) | NOT NULL | Valor da transação |
| `balance_after` | DECIMAL(15,2) | NOT NULL | Saldo após transação |
| `description` | VARCHAR(255) | | Descrição adicional |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |

**Relacionamentos:**
- `bankroll_id` → `core.bankrolls(id)` ON DELETE CASCADE
- `ticket_id` → `betting.bet_tickets(id)` ON DELETE SET NULL

**Índices:**
- `idx_bankroll_transactions_bankroll_id` ON (bankroll_id)
- `idx_bankroll_transactions_ticket_id` ON (ticket_id)
- `idx_bankroll_transactions_type` ON (type)
- `idx_bankroll_transactions_created_at` ON (created_at)

---

### Schema: `betting`

#### 5. `betting.bet_tickets`
Bilhetes de apostas (tickets).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único do ticket |
| `user_id` | BIGINT | NOT NULL, FK → users.id | Dono do ticket |
| `provider_id` | BIGINT | NOT NULL, FK → betting_providers.id | Casa de aposta |
| `bankroll_id` | BIGINT | FK → bankrolls.id | Bankroll associado |
| `external_ticket_id` | VARCHAR(100) | | ID do ticket na casa de aposta |
| `source_url` | VARCHAR(500) | | URL original do ticket |
| `bet_type` | VARCHAR(50) | NOT NULL, DEFAULT 'SINGLE' | Tipo (SINGLE, MULTIPLE, SYSTEM) |
| `bet_side` | VARCHAR(50) | NOT NULL, DEFAULT 'BACK' | Lado (BACK, LAY) |
| `stake` | DECIMAL(15,2) | NOT NULL | Valor apostado |
| `total_odd` | DECIMAL(10,4) | NOT NULL | Odd total |
| `potential_payout` | DECIMAL(15,2) | | Retorno potencial |
| `actual_payout` | DECIMAL(15,2) | | Retorno real |
| `ticket_status` | VARCHAR(50) | NOT NULL, DEFAULT 'OPEN' | Status (OPEN, WON, LOST, VOID, CASHED_OUT) |
| `financial_status` | VARCHAR(50) | NOT NULL, DEFAULT 'PENDING' | Status financeiro (PENDING, FULL_WIN, PARTIAL_WIN, BREAK_EVEN, PARTIAL_LOSS, TOTAL_LOSS) |
| `profit_loss` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Lucro/prejuízo |
| `roi` | DECIMAL(10,4) | NOT NULL, DEFAULT 0 | ROI (Return on Investment) |
| `system_description` | VARCHAR(50) | | Descrição do sistema (ex: "3/5") |
| `placed_at` | BIGINT | | Data da aposta (ms) |
| `settled_at` | BIGINT | | Data de conclusão (ms) |
| `is_cashed_out` | BOOLEAN | DEFAULT FALSE | Indica se fez cashout |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |
| `updated_at` | BIGINT | NOT NULL | Timestamp de atualização (ms) |

**Relacionamentos:**
- `user_id` → `core.users(id)` ON DELETE CASCADE
- `provider_id` → `core.betting_providers(id)` ON DELETE RESTRICT
- `bankroll_id` → `core.bankrolls(id)` ON DELETE SET NULL

**Constraints:**
- `uk_ticket_user_external` UNIQUE (user_id, external_ticket_id)

**Índices (V1):**
- `idx_bet_tickets_user_id` ON (user_id)
- `idx_bet_tickets_provider_id` ON (provider_id)
- `idx_bet_tickets_bankroll_id` ON (bankroll_id)
- `idx_bet_tickets_external_id` ON (external_ticket_id)
- `idx_bet_tickets_status` ON (ticket_status)
- `idx_bet_tickets_financial_status` ON (financial_status)
- `idx_bet_tickets_placed_at` ON (placed_at)

**Índices Adicionais (V8 - Performance):**
- `idx_bet_tickets_user_status` ON (user_id, ticket_status)
- `idx_bet_tickets_external_provider` UNIQUE ON (external_ticket_id, provider_id) WHERE external_ticket_id IS NOT NULL
- `idx_bet_tickets_user_financial_status` ON (user_id, financial_status)
- `idx_bet_tickets_provider_status` ON (provider_id, ticket_status)

---

#### 6. `betting.bet_selections`
Seleções individuais de cada ticket (pernas da aposta).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único da seleção |
| `ticket_id` | BIGINT | NOT NULL, FK → bet_tickets.id | Ticket relacionado |
| `tournament_id` | BIGINT | FK → tournaments.id | Torneio/competição |
| `external_selection_id` | VARCHAR(100) | | ID da seleção no provider |
| `event_name` | VARCHAR(255) | NOT NULL | Nome do evento |
| `market_type` | VARCHAR(100) | | Tipo de mercado |
| `selection` | VARCHAR(255) | NOT NULL | Seleção escolhida |
| `odd` | DECIMAL(10,4) | NOT NULL | Odd da seleção |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'PENDING' | Status (PENDING, WON, LOST, VOID, HALF_WON, HALF_LOST) |
| `event_date` | BIGINT | | Data do evento (ms) |
| `event_result` | VARCHAR(100) | | Resultado do evento |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |
| `updated_at` | BIGINT | NOT NULL | Timestamp de atualização (ms) |

**Relacionamentos:**
- `ticket_id` → `betting.bet_tickets(id)` ON DELETE CASCADE
- `tournament_id` → `betting.tournaments(id)` ON DELETE SET NULL

**Índices:**
- `idx_bet_selections_ticket_id` ON (ticket_id)
- `idx_bet_selections_status` ON (status)
- `idx_bet_selections_market` ON (market_type)
- `idx_bet_selections_tournament_id` ON (tournament_id)
- `idx_bet_selections_event_name` ON (event_name) -- V8
- `idx_bet_selections_ticket_market` ON (ticket_id, market_type) -- V8

---

#### 7. `betting.bet_selection_components`
Componentes de Bet Builder (quando uma seleção tem múltiplos mercados no mesmo evento).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único do componente |
| `selection_id` | BIGINT | NOT NULL, FK → bet_selections.id | Seleção relacionada |
| `market_id` | VARCHAR(50) | | ID do mercado |
| `market_name` | VARCHAR(255) | NOT NULL | Nome do mercado |
| `selection_name` | VARCHAR(255) | NOT NULL | Nome da seleção |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'PENDING' | Status do componente |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |

**Relacionamentos:**
- `selection_id` → `betting.bet_selections(id)` ON DELETE CASCADE

**Índices:**
- `idx_components_selection_id` ON (selection_id)
- `idx_components_selection_status` ON (selection_id, status) -- V8

---

#### 8. `betting.sports`
Esportes suportados (futebol, basquete, etc).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único do esporte |
| `provider_id` | BIGINT | NOT NULL, FK → betting_providers.id | Provider relacionado |
| `external_id` | INT | NOT NULL | ID do esporte no provider |
| `name` | VARCHAR(100) | NOT NULL | Nome do esporte |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |
| `updated_at` | BIGINT | NOT NULL | Timestamp de atualização (ms) |

**Relacionamentos:**
- `provider_id` → `core.betting_providers(id)` ON DELETE CASCADE

**Constraints:**
- `uk_sport_provider_external` UNIQUE (provider_id, external_id)

**Índices:**
- `idx_sports_provider_id` ON (provider_id)

---

#### 9. `betting.tournaments`
Torneios/competições (Premier League, Champions League, etc).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único do torneio |
| `provider_id` | BIGINT | NOT NULL, FK → betting_providers.id | Provider relacionado |
| `sport_id` | BIGINT | NOT NULL, FK → sports.id | Esporte do torneio |
| `external_id` | INT | NOT NULL | ID do torneio no provider |
| `name` | VARCHAR(255) | NOT NULL | Nome do torneio |
| `local_name` | VARCHAR(255) | | Nome da categoria/país (V6) |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |
| `updated_at` | BIGINT | NOT NULL | Timestamp de atualização (ms) |

**Relacionamentos:**
- `provider_id` → `core.betting_providers(id)` ON DELETE CASCADE
- `sport_id` → `betting.sports(id)` ON DELETE CASCADE

**Constraints:**
- `uk_tournament_provider_external` UNIQUE (provider_id, external_id)

**Índices:**
- `idx_tournaments_provider_id` ON (provider_id)
- `idx_tournaments_sport_id` ON (sport_id)
- `idx_tournaments_external_id` ON (external_id)
- `idx_tournaments_local_name` ON (local_name) -- V6

---

### Schema: `public`

#### 10. `public.provider_api_requests`
Log de requisições às APIs dos providers (auditoria).

| Campo | Tipo | Constraints | Descrição |
|-------|------|-------------|-----------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único da requisição |
| `user_id` | BIGINT | FK → users.id | Usuário que fez a requisição |
| `url` | VARCHAR(500) | NOT NULL | URL chamada |
| `provider_name` | VARCHAR(100) | | Nome do provider |
| `request_count` | INT | NOT NULL, DEFAULT 1 | Contador de requisições |
| `last_requested_at` | BIGINT | NOT NULL | Última requisição (ms) |
| `created_at` | BIGINT | NOT NULL | Timestamp de criação (ms) |

**Relacionamentos:**
- `user_id` → `core.users(id)` ON DELETE SET NULL

**Índices:**
- `idx_provider_api_requests_provider_name` ON (provider_name)
- `idx_provider_api_requests_request_count` ON (request_count DESC)

---

## 🔗 Diagrama de Relacionamentos

```
core.users (1) ──────< (N) core.bankrolls (N) ────── (1) core.betting_providers
     │                           │                               │
     │                           │                               │
     └──────< (N) betting.bet_tickets (N) ────────────────────────┘
                      │          │
                      │          └────< (N) core.bankroll_transactions
                      │
                      └───< (N) betting.bet_selections
                                       │         │
                                       │         └────< (N) betting.tournaments (N) ──── (1) betting.sports
                                       │                                                          │
                                       │                                                          └──── (1) core.betting_providers
                                       │
                                       └────< (N) betting.bet_selection_components
```

---

## 📈 Índices de Performance (V8)

### Índices Críticos para Produção

| Índice | Tabela | Colunas | Tipo | Motivo |
|--------|--------|---------|------|--------|
| `idx_bet_tickets_user_id` | bet_tickets | user_id | Simples | Listagem de tickets |
| `idx_bet_tickets_user_status` | bet_tickets | user_id, ticket_status | Composto | Refresh de tickets OPEN |
| `idx_bet_tickets_external_provider` | bet_tickets | external_ticket_id, provider_id | UNIQUE | Previne duplicatas |
| `idx_bet_selections_ticket_id` | bet_selections | ticket_id | Simples | JOIN frequente |
| `idx_bet_tickets_user_financial_status` | bet_tickets | user_id, financial_status | Composto | Analytics |
| `idx_bet_tickets_provider_status` | bet_tickets | provider_id, ticket_status | Composto | Analytics por provider |

**Ganho estimado:** 10-100x em queries de listagem e analytics

---

## 🔒 Constraints e Regras de Integridade

### Constraints Únicas

| Tabela | Constraint | Colunas | Propósito |
|--------|-----------|---------|-----------|
| `core.users` | UNIQUE | email | Email único por usuário |
| `core.users` | UNIQUE | external_id | ID externo único |
| `core.betting_providers` | UNIQUE | slug | Slug único por provider |
| `betting.bet_tickets` | UNIQUE | user_id, external_ticket_id | Previne duplicatas de import |
| `betting.sports` | UNIQUE | provider_id, external_id | Esporte único por provider |
| `betting.tournaments` | UNIQUE | provider_id, external_id | Torneio único por provider |

### Foreign Keys com Cascade

| Tabela Origem | Coluna | Tabela Destino | ON DELETE |
|---------------|--------|----------------|-----------|
| `core.bankrolls` | user_id | core.users | CASCADE |
| `betting.bet_tickets` | user_id | core.users | CASCADE |
| `betting.bet_tickets` | provider_id | core.betting_providers | RESTRICT |
| `betting.bet_selections` | ticket_id | betting.bet_tickets | CASCADE |
| `betting.bet_selection_components` | selection_id | betting.bet_selections | CASCADE |
| `betting.sports` | provider_id | core.betting_providers | CASCADE |
| `betting.tournaments` | provider_id | core.betting_providers | CASCADE |
| `betting.tournaments` | sport_id | betting.sports | CASCADE |

---

## 📊 Enums Utilizados (Application Level)

### TicketStatus
- `OPEN` - Aposta em aberto
- `WON` - Ganhou
- `LOST` - Perdeu
- `VOID` - Anulada
- `CASHED_OUT` - Encerrada via cashout

### FinancialStatus
- `PENDING` - Pendente
- `FULL_WIN` - Ganho total
- `PARTIAL_WIN` - Ganho parcial
- `BREAK_EVEN` - Empate (sem lucro/prejuízo)
- `PARTIAL_LOSS` - Prejuízo parcial
- `TOTAL_LOSS` - Prejuízo total

### SelectionStatus
- `PENDING` - Pendente
- `WON` - Ganhou
- `LOST` - Perdeu
- `VOID` - Anulada
- `HALF_WON` - Meio ganho (Asian Handicap)
- `HALF_LOST` - Meio perdido (Asian Handicap)

### BetType
- `SINGLE` - Aposta simples
- `MULTIPLE` - Aposta múltipla
- `SYSTEM` - Aposta sistema

### BetSide
- `BACK` - A favor
- `LAY` - Contra

### TransactionType
- `DEPOSIT` - Depósito
- `WITHDRAW` - Saque
- `BET` - Aposta
- `WIN` - Ganho
- `LOSS` - Perda

---

## 📝 Notas de Implementação

1. **Timestamps:** Todos os timestamps são armazenados como `BIGINT` em milissegundos (Unix epoch * 1000)
2. **Decimal Precision:** Valores monetários usam `DECIMAL(15,2)` e odds usam `DECIMAL(10,4)`
3. **Schemas:** Separação lógica entre dados de core, betting e logs
4. **Soft Deletes:** Não implementado - usa `is_active` onde necessário
5. **Auditoria:** Todas as tabelas principais têm `created_at` e `updated_at`
6. **Idempotência:** Migrations usam `IF NOT EXISTS` e `IF EXISTS` para segurança

---

**Fim da Documentação**
