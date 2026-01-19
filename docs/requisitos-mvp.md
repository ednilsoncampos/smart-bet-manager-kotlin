# Requisitos do MVP - Smart Bet Manager

## Visão Geral

O Smart Bet Manager é uma aplicação para gerenciamento de apostas esportivas, permitindo aos usuários importar bilhetes de casas de apostas, gerenciar bancas e analisar performance.

## Funcionalidades do MVP

### 1. Autenticação e Usuários

- [x] Registro de usuário (email/senha)
- [x] Login com JWT
- [x] Refresh token
- [x] Alteração de senha
- [x] Logout

### 2. Gerenciamento de Bancas (Bankrolls)

- [x] Criar banca vinculada a uma casa de apostas
- [x] Listar bancas do usuário
- [x] Visualizar detalhes de uma banca
- [x] Registrar transações (depósito, saque, bônus)
- [x] Histórico de transações
- [x] Resumo consolidado de todas as bancas
- [x] Desativar banca (soft delete)

### 3. Gerenciamento de Bilhetes (Tickets)

- [x] Importar bilhete via URL compartilhada
- [x] Criar bilhete manualmente
- [x] Listar bilhetes com filtros (status, financeiro, casa)
- [x] Visualizar detalhes do bilhete
- [x] Atualizar status do bilhete
- [x] Deletar bilhete
- [x] Reimportar bilhetes em aberto (refresh automático)

### 4. Casas de Apostas (Providers)

- [x] Listar todas as casas cadastradas
- [x] Listar casas ativas
- [x] Verificar se URL é suportada
- [x] Parsers implementados:
  - [x] Betano
  - [x] Superbet

### 5. Analytics

- [x] Performance geral (ROI, lucro, win rate)
- [x] Performance por torneio
- [x] Performance por tipo de mercado
- [x] Performance por casa de apostas
- [x] Evolução da banca ao longo do tempo

## Regras de Negócio

### Cálculo de Saldo da Banca

O saldo da banca é calculado com base em:
- Depósitos (+)
- Saques (-)
- Bônus (+)
- Apostas ganhas (+)
- Apostas perdidas (-)
- Cashback (+)

### Status do Bilhete

| Status | Descrição |
|--------|-----------|
| OPEN | Aposta em andamento |
| WON | Aposta ganha |
| LOST | Aposta perdida |
| VOID | Aposta cancelada/anulada |
| PARTIAL | Parcialmente ganha/perdida |
| CASHOUT | Cashout realizado |

### Status Financeiro

| Status | Descrição |
|--------|-----------|
| PENDING | Aguardando resultado |
| SETTLED | Liquidado |

## Requisitos Não-Funcionais

### Performance
- Tempo de resposta < 500ms para APIs
- Suporte a 1000 usuários simultâneos

### Segurança
- Autenticação JWT
- Senhas hasheadas com BCrypt
- CORS configurado
- Validação de entrada em todos os endpoints

### Disponibilidade
- 99.5% uptime
- Health checks configurados
- Graceful shutdown

### Observabilidade
- Logs estruturados (JSON)
- Métricas Prometheus
- Health probes (liveness/readiness)

### 6. Atualização Automática de Bilhetes

- [x] API assíncrona para refresh de bilhetes em aberto
- [x] Job agendado a cada 30 minutos
- [x] Suporte a múltiplos providers
- [x] Tratamento de erros e retry

#### Fluxo de Atualização

1. **No Login do App:**
   - App chama `POST /api/tickets/refresh-open`
   - Backend retorna `202 Accepted` com quantidade de bilhetes
   - App mostra toast "Atualizando N bilhetes..." por 5-10s
   - Processamento ocorre em background

2. **Job Agendado (30 min):**
   - Busca todos os bilhetes OPEN com sourceUrl
   - Reimporta de cada provider
   - Atualiza status, payout, ROI
   - Loga resultado e erros

#### Endpoint

```
POST /api/tickets/refresh-open
Authorization: Bearer {token}

Response 202:
{
  "message": "Atualizando 5 bilhete(s) em background",
  "ticketsToRefresh": 5,
  "status": "PROCESSING"
}
```

## Próximas Funcionalidades (Pós-MVP)

- [ ] Notificações push
- [ ] Integração com mais casas de apostas
- [ ] Relatórios exportáveis (PDF/Excel)
- [ ] Metas e alertas personalizados
- [ ] Modo offline no mobile
- [x] ~~Sincronização automática de resultados~~ (Implementado)
