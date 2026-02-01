# 📊 Análise Completa: Todos os Endpoints de Analytics

## 🎯 Visão Geral

Este documento analisa TODOS os endpoints da tela de analytics após as correções implementadas.

---

## 📍 Endpoints Disponíveis

| # | Endpoint | Aba na Tela | Status |
|---|----------|------------|--------|
| 1 | `GET /api/analytics/overall` | Dashboard/Geral | ✅ CORRETO |
| 2 | `GET /api/analytics/by-tournament` | Por Torneio | ✅ CORRETO |
| 3 | `GET /api/analytics/by-market` | Por Mercado | ⚠️ PENDENTE |
| 4 | `GET /api/analytics/by-provider` | Por Casa | ✅ CORRETO |
| 5 | `GET /api/analytics/by-month` | Por Mês | ✅ CORRETO |
| 6 | `GET /api/analytics/bankroll-evolution` | Evolução de Saldo | ✅ CORRETO |

---

## 1️⃣ GET /api/analytics/overall (Dashboard/Geral)

### Descrição
Performance geral consolidada do usuário.

### Campos do Response
```kotlin
OverallPerformanceResponse(
    totalBets: Long,

    // Contadores Agregados
    wins: Long,              // full_won + partial_won
    losses: Long,            // partial_lost + total_lost
    voids: Long,             // break_even
    cashedOut: Long,

    // Contadores Granulares
    fullWins: Long,          // FULL_WIN
    partialWins: Long,       // PARTIAL_WIN
    breakEven: Long,         // BREAK_EVEN
    partialLosses: Long,     // PARTIAL_LOSS
    totalLosses: Long,       // TOTAL_LOSS

    // Métricas Principais
    winRate: BigDecimal,     // (fullWins / totalBets) * 100
    successRate: BigDecimal, // ((fullWins + partialWins) / totalBets) * 100
    totalStaked: BigDecimal,
    totalReturns: BigDecimal,
    profitLoss: BigDecimal,
    roi: BigDecimal,         // (profit / stake) * 100
    avgOdd: BigDecimal?,
    avgStake: BigDecimal?,   // ✅ ADICIONADO

    // Métricas Granulares Derivadas
    fullWinRate: BigDecimal,
    partialWinRate: BigDecimal,
    breakEvenRate: BigDecimal,
    partialLossRate: BigDecimal,
    totalLossRate: BigDecimal,
    cashoutSuccessRate: BigDecimal?,

    // Gamificação
    currentStreak: Int,
    bestWinStreak: Int,
    worstLossStreak: Int,
    biggestWin: BigDecimal?,
    biggestLoss: BigDecimal?,
    bestRoiTicket: BigDecimal?,

    // Timestamps
    firstBetAt: Long?,
    lastSettledAt: Long
)
```

### ✅ Correções Aplicadas
1. **Taxas recalculadas**: Lê diretamente dos contadores em vez de valores armazenados
2. **avgStake presente**: Campo implementado e calculado corretamente

### 📊 Fonte dos Dados
- Tabela: `analytics.performance_overall`
- Agregação: `AnalyticsAggregationService.updateOnSettlement()`
- Lógica: Valores COMPLETOS do ticket (não há divisão)

### ✅ Status: CORRETO

---

## 2️⃣ GET /api/analytics/by-tournament (Por Torneio)

### Descrição
Performance agrupada por torneio/campeonato. Um ticket com seleções em múltiplos torneios cria um registro para CADA torneio.

### Campos do Response
```kotlin
PerformanceByTournamentResponse(
    tournamentId: Long,
    tournamentName: String,
    tournamentLocalName: String?,
    totalBets: Long,

    // Contadores Agregados
    wins: Long,
    losses: Long,
    voids: Long,

    // Contadores Granulares
    fullWins: Long,
    partialWins: Long,
    breakEven: Long,
    partialLosses: Long,
    totalLosses: Long,

    // Métricas
    winRate: BigDecimal,     // ✅ RECALCULADO
    successRate: BigDecimal, // ✅ RECALCULADO
    totalStaked: BigDecimal,
    profitLoss: BigDecimal,
    roi: BigDecimal,         // ✅ RECALCULADO
    avgOdd: BigDecimal?,

    // Métricas Granulares
    fullWinRate: BigDecimal,
    partialWinRate: BigDecimal,
    partialLossRate: BigDecimal,
    totalLossRate: BigDecimal,

    // Timestamps
    firstBetAt: Long?,
    lastSettledAt: Long
)
```

### ✅ Correções Aplicadas
1. **Taxas recalculadas** no service (linha 144-149 de `PerformanceAnalyticService.kt`):
   ```kotlin
   winRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
   successRate = calculateRate(performance.ticketsWon, performance.totalTickets),
   roi = calculateRoi(performance.totalProfit, performance.totalStake),
   ```

2. **marketType nunca null** - Bug crítico corrigido em `SuperbetStrategy.kt:378`:
   ```kotlin
   val marketName = event.path("market").path("name").asText()
       .takeIf { it.isNotEmpty() }
       ?: "Mercado Desconhecido"  // ✅ Nunca null!
   ```

### 📊 Fonte dos Dados
- Tabela: `analytics.performance_by_tournament`
- Agregação: `AnalyticsAggregationService.updateByTournament()`
- Lógica: **Valor COMPLETO do ticket** em cada registro de torneio

### 💡 Comportamento Esperado
**Ticket com 3 torneios diferentes:**
- Stake: R$ 30
- Cria 3 registros em `performance_by_tournament`
- **Cada registro tem**: `totalStake = R$ 30` (valor completo)
- **Soma dos stakes**: R$ 90 (3 × R$ 30)

**Isso é CORRETO segundo a decisão do usuário:**
> "por torneio deve ser baseada no ticket"

### ✅ Status: CORRETO

---

## 3️⃣ GET /api/analytics/by-market (Por Mercado)

### Descrição
Performance agrupada por tipo de mercado (1X2, Handicap, Total de Gols, etc.). Suporta expansão de componentes do Bet Builder.

### Campos do Response
```kotlin
PerformanceByMarketResponse(
    marketType: String,
    totalSelections: Long,   // Total de seleções neste mercado
    uniqueTickets: Long,     // Número de tickets únicos

    // Contadores de Seleções
    wins: Long,              // Seleções ganhas
    losses: Long,            // Seleções perdidas
    voids: Long,             // Seleções anuladas

    // Contadores de Tickets (FinancialStatus)
    fullWins: Long,
    partialWins: Long,
    breakEven: Long,
    partialLosses: Long,
    totalLosses: Long,

    // Métricas
    winRate: BigDecimal,     // (wins / totalSelections) * 100
    successRate: BigDecimal, // ((fullWins + partialWins) / uniqueTickets) * 100
    totalStaked: BigDecimal,
    profitLoss: BigDecimal,
    roi: BigDecimal,
    avgOdd: BigDecimal?,

    // Métricas Granulares
    fullWinRate: BigDecimal,
    partialWinRate: BigDecimal,
    partialLossRate: BigDecimal,
    totalLossRate: BigDecimal,

    // Timestamps
    firstBetAt: Long?,
    lastSettledAt: Long,

    // Bet Builder
    betBuilderComponents: List<BetBuilderComponentStats>?
)
```

### ⚠️ PROBLEMA PENDENTE: Divisão Proporcional

**Código Atual** (`AnalyticsAggregationService.kt:280-309`):
```kotlin
private fun createNewByMarket(id: PerformanceByMarketId, event: TicketSettledEvent): PerformanceByMarketEntity {
    // ...
    return PerformanceByMarketEntity(
        // ...
        totalStake = event.stake,         // ← VALOR COMPLETO (ERRADO!)
        totalProfit = event.profitLoss,   // ← VALOR COMPLETO (ERRADO!)
        roi = event.roi,                  // ← BASEADO NO VALOR COMPLETO (ERRADO!)
        // ...
    )
}
```

**Mesmo problema em** `updateExistingByMarket()` (linha 433-462):
```kotlin
entity.totalStake += event.stake        // ← VALOR COMPLETO (ERRADO!)
entity.totalProfit += event.profitLoss  // ← VALOR COMPLETO (ERRADO!)
```

### 🔧 Correção Necessária

**Ticket com 3 mercados diferentes:**
- 2 seleções "Handicap"
- 1 seleção "Total de Gols"
- **Total**: 3 seleções
- Stake: R$ 30

**Lógica Proporcional:**
```kotlin
// Conta seleções do ticket neste mercado
val selectionsInMarket = event.selections.count { it.marketType == marketType }
val totalSelections = event.selections.size

// Calcula proporção
val proportion = if (totalSelections > 0)
    BigDecimal(selectionsInMarket).divide(BigDecimal(totalSelections), 4, RoundingMode.HALF_UP)
    else BigDecimal.ONE

// Aplica proporção
val proportionalStake = event.stake.multiply(proportion)
val proportionalProfit = event.profitLoss.multiply(proportion)
val proportionalRoi = calculateRoi(proportionalProfit, proportionalStake)
```

**Resultado Esperado:**
- **Handicap**: `stake = R$ 30 × (2/3) = R$ 20`
- **Total de Gols**: `stake = R$ 30 × (1/3) = R$ 10`
- **Soma**: R$ 20 + R$ 10 = **R$ 30** ✅

### 📊 Fonte dos Dados
- Tabela: `analytics.performance_by_market`
- Agregação: `AnalyticsAggregationService.updateByMarket()`

### ⚠️ Status: **PENDENTE - Implementar divisão proporcional**

---

## 4️⃣ GET /api/analytics/by-provider (Por Casa)

### Descrição
Performance agrupada por casa de apostas (Superbet, Betano, etc.).

### Campos do Response
```kotlin
PerformanceByProviderResponse(
    providerId: Long,
    providerName: String,
    totalBets: Long,

    // Contadores Agregados
    wins: Long,
    losses: Long,
    voids: Long,
    cashedOut: Long,

    // Contadores Granulares
    fullWins: Long,
    partialWins: Long,
    breakEven: Long,
    partialLosses: Long,
    totalLosses: Long,

    // Métricas
    winRate: BigDecimal,     // ✅ RECALCULADO
    successRate: BigDecimal, // ✅ RECALCULADO
    totalStaked: BigDecimal,
    profitLoss: BigDecimal,
    roi: BigDecimal,         // ✅ RECALCULADO
    avgOdd: BigDecimal?,
    avgStake: BigDecimal?,   // ✅ ADICIONADO

    // Métricas Granulares
    fullWinRate: BigDecimal,
    partialWinRate: BigDecimal,
    partialLossRate: BigDecimal,
    totalLossRate: BigDecimal,
    cashoutSuccessRate: BigDecimal?,

    // Timestamps
    firstBetAt: Long?,
    lastSettledAt: Long
)
```

### ✅ Correções Aplicadas
1. **Taxas recalculadas** (linha 510-515 de `PerformanceAnalyticService.kt`):
   ```kotlin
   winRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
   successRate = calculateRate(performance.ticketsWon, performance.totalTickets),
   roi = calculateRoi(performance.totalProfit, performance.totalStake),
   ```

2. **Campo avgStake implementado**:
   - Migration: `V1__create_complete_schema.sql` - coluna adicionada
   - Entity: `PerformanceByProviderEntity.kt` - campo adicionado
   - DTO: `PerformanceAnalyticDto.kt` - campo adicionado
   - Service: `PerformanceAnalyticService.kt` - mapeamento adicionado (linha 517)
   - Aggregation: `AnalyticsAggregationService.kt` - cálculo incremental adicionado (linha 428)

### 📊 Fonte dos Dados
- Tabela: `analytics.performance_by_provider`
- Agregação: `AnalyticsAggregationService.updateByProvider()`
- Lógica: Valores COMPLETOS do ticket (não há divisão)

### ✅ Status: CORRETO

---

## 5️⃣ GET /api/analytics/by-month (Por Mês)

### Descrição
Performance agrupada por mês/ano, ordenada do mais recente para o mais antigo.

### Campos do Response
```kotlin
PerformanceByMonthResponse(
    year: Int,
    month: Int,
    totalBets: Long,

    // Contadores Agregados
    wins: Long,
    losses: Long,
    voids: Long,

    // Contadores Granulares
    fullWins: Long,
    partialWins: Long,
    breakEven: Long,
    partialLosses: Long,
    totalLosses: Long,

    // Métricas
    winRate: BigDecimal,     // ✅ RECALCULADO
    successRate: BigDecimal, // ✅ RECALCULADO
    totalStaked: BigDecimal,
    profitLoss: BigDecimal,
    roi: BigDecimal,         // ✅ RECALCULADO
    avgStake: BigDecimal?,

    // Métricas Granulares
    fullWinRate: BigDecimal,
    partialWinRate: BigDecimal,
    partialLossRate: BigDecimal,
    totalLossRate: BigDecimal,

    // Timestamps
    firstBetAt: Long?,
    lastSettledAt: Long
)
```

### ✅ Correções Aplicadas
1. **Taxas recalculadas** (linha 182-187 de `PerformanceAnalyticService.kt`):
   ```kotlin
   winRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
   successRate = calculateRate(performance.ticketsWon, performance.totalTickets),
   roi = calculateRoi(performance.totalProfit, performance.totalStake),
   ```

2. **avgStake presente** (linha 188)

### 📊 Fonte dos Dados
- Tabela: `analytics.performance_by_month`
- Agregação: `AnalyticsAggregationService.updateByMonth()`
- Lógica: Valores COMPLETOS do ticket (não há divisão)

### ✅ Status: CORRETO

---

## 6️⃣ GET /api/analytics/bankroll-evolution

### Descrição
Evolução do saldo consolidado de todas as bancas do usuário ao longo do tempo.

### Parâmetros
- `startDate` (opcional): Timestamp inicial
- `endDate` (opcional): Timestamp final
- `granularity` (opcional): `day`, `week`, `month` (padrão: `day`)

### ✅ Status: CORRETO
Este endpoint não foi afetado pelas correções, pois lida com evolução de saldo, não com performance de tickets.

---

## 📋 Resumo das Correções Implementadas

### ✅ Correção 1: Taxa Zerada
**Arquivos:** `PerformanceAnalyticService.kt`

**O que foi corrigido:**
- Métodos `getPerformanceByTournament()`, `getPerformanceByMonth()`, `getPerformanceByProvider()` agora **recalculam** `winRate`, `successRate` e `roi` em vez de usar valores armazenados no banco.

**Linhas afetadas:**
- `PerformanceAnalyticService.kt:144-149` (by-tournament)
- `PerformanceAnalyticService.kt:182-187` (by-month)
- `PerformanceAnalyticService.kt:510-515` (by-provider)

---

### ✅ Correção 2: Campo avgStake Faltando
**Arquivos:** Migration, Entity, DTO, Service, Aggregation

**O que foi implementado:**
1. **Migration**: Adicionada coluna `avg_stake DECIMAL(10,2)` em `performance_by_provider`
2. **Entity**: Campo `avgStake: BigDecimal?` em `PerformanceByProviderEntity`
3. **DTO**: Campo `avgStake` em `PerformanceByProviderResponse`
4. **Service**: Mapeamento do campo (linha 517)
5. **Aggregation**: Cálculo incremental da média (linha 428)

---

### ✅ Correção 3: Bug Crítico - marketType Null
**Arquivo:** `SuperbetStrategy.kt:376-378`

**O que foi corrigido:**
```kotlin
// ANTES
val marketName = event.path("market").path("name").asText()
    .takeIf { it.isNotEmpty() }  // ← Retornava null!

// DEPOIS
val marketName = event.path("market").path("name").asText()
    .takeIf { it.isNotEmpty() }
    ?: "Mercado Desconhecido"  // ← Nunca null!
```

**Impacto:** Tickets com `market.name = ""` na API agora aparecem corretamente em todas as abas de analytics.

---

## ⚠️ Correção Pendente: Divisão Proporcional por Mercado

### O Que Precisa Ser Feito

**Arquivos a modificar:**
1. `AnalyticsAggregationService.kt:280-309` - `createNewByMarket()`
2. `AnalyticsAggregationService.kt:433-462` - `updateExistingByMarket()`

**Lógica a implementar:**
```kotlin
// Conta seleções do ticket neste mercado
val selectionsInMarket = event.selections.count { it.marketType == marketType }
val totalSelections = event.selections.size

// Calcula proporção
val proportion = if (totalSelections > 0)
    BigDecimal(selectionsInMarket).divide(BigDecimal(totalSelections), 4, RoundingMode.HALF_UP)
    else BigDecimal.ONE

// Aplica proporção aos valores financeiros
val proportionalStake = event.stake.multiply(proportion)
val proportionalProfit = event.profitLoss.multiply(proportion)
val proportionalRoi = calculateRoi(proportionalProfit, proportionalStake)
```

**Substituir:**
- `totalStake = event.stake` → `totalStake = proportionalStake`
- `totalProfit = event.profitLoss` → `totalProfit = proportionalProfit`
- `roi = event.roi` → `roi = proportionalRoi`

### Por Que É Importante

**Sem divisão proporcional:**
- Ticket com 3 mercados, stake R$ 30
- Cada mercado conta R$ 30
- **Soma**: R$ 90 (inflado!)

**Com divisão proporcional:**
- Ticket com 3 mercados, stake R$ 30
- Cada mercado conta proporcionalmente
- **Soma**: R$ 30 ✅

---

## 🎯 Validação Pós-Lançamento

Após recriar o banco e importar tickets de teste:

### Dashboard (Overall)
```
✅ Deve mostrar 2 apostas
✅ Total stake: R$ 75,00
✅ Total profit: R$ 115,98
✅ winRate e successRate: NÃO devem estar zerados
✅ avgStake: R$ 37,50
```

### Aba "Por Torneio"
```
✅ Deve mostrar vários torneios
✅ Cada torneio com wins/losses corretos
✅ winRate e successRate: NÃO devem estar zerados
✅ Stake de cada torneio = stake completo do ticket
```

### Aba "Por Casa"
```
✅ Deve mostrar: Superbet
✅ avgStake: R$ 37,50 (75 / 2)
✅ avgOdd: Média das odds
✅ winRate e successRate: NÃO devem estar zerados
```

### Aba "Por Mercado"
```
✅ Deve mostrar vários mercados
✅ Se tiver "Mercado Desconhecido": Bug corrigido funcionou!
⚠️ Soma dos stakes: Pode estar inflada (até implementar divisão proporcional)
```

### Aba "Por Mês"
```
✅ Deve mostrar mês atual com 2 apostas
✅ Total stake: R$ 75,00
✅ winRate e successRate: NÃO devem estar zerados
✅ avgStake: R$ 37,50
```

---

## 📊 Matriz de Compatibilidade

| Endpoint | Taxa Recalculada | avgStake | marketType Fix | Divisão Proporcional |
|----------|------------------|----------|----------------|---------------------|
| Overall | N/A | ✅ | ✅ | N/A |
| By Tournament | ✅ | N/A | ✅ | N/A (usa completo) |
| By Market | N/A | N/A | ✅ | ⚠️ PENDENTE |
| By Provider | ✅ | ✅ | ✅ | N/A (usa completo) |
| By Month | ✅ | ✅ | ✅ | N/A (usa completo) |
| Bankroll Evolution | N/A | N/A | N/A | N/A |

---

## 🚀 Próximo Passo

**ANTES de recriar o banco:**
1. ⬜ Implementar divisão proporcional em `AnalyticsAggregationService.kt`
   - Modificar `createNewByMarket()` (linha 280-309)
   - Modificar `updateExistingByMarket()` (linha 433-462)

**DEPOIS de implementar:**
2. ⬜ Recriar banco de dados
3. ⬜ Importar tickets de teste
4. ⬜ Validar todos os endpoints conforme checklist acima

---

## 📝 Conclusão

**Status Atual:**
- ✅ 5 de 6 endpoints completamente corretos
- ⚠️ 1 endpoint com correção pendente (By Market - divisão proporcional)
- ✅ Todos os bugs críticos identificados foram corrigidos
- ✅ Sistema pronto para lançamento após implementar última correção
