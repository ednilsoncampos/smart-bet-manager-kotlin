# ✅ Correção Implementada: Divisão Proporcional por Mercado

## 🎯 Problema Resolvido

**Antes da correção:**
- Ticket com 3 mercados diferentes, stake R$ 30
- Cada mercado contava R$ 30 completo
- **Soma total**: R$ 90 (inflado 3x!)

**Depois da correção:**
- Ticket com 3 mercados diferentes, stake R$ 30
- Cada mercado conta proporcionalmente
- **Soma total**: R$ 30 ✅

---

## 🔧 Implementação

### Arquivo Modificado
`src/main/kotlin/com/smartbet/application/usecase/AnalyticsAggregationService.kt`

### Métodos Alterados
1. `createNewByMarket()` (linha ~280-325)
2. `updateExistingByMarket()` (linha ~433-480)

---

## 📊 Lógica de Divisão Proporcional

### Cálculo da Proporção

```kotlin
// Conta seleções do ticket neste mercado específico
val selectionsInMarket = event.selections.count { it.marketType == id.marketType }

// Total de seleções no ticket
val totalSelections = event.selections.size

// Calcula a proporção com 4 casas decimais
val proportion = if (totalSelections > 0) {
    BigDecimal(selectionsInMarket).divide(BigDecimal(totalSelections), 4, RoundingMode.HALF_UP)
} else {
    BigDecimal.ONE  // Fallback: se não há seleções, usa 100%
}
```

### Aplicação da Proporção

```kotlin
// Multiplica stake e profit pela proporção
val proportionalStake = event.stake.multiply(proportion).setScale(2, RoundingMode.HALF_UP)
val proportionalProfit = event.profitLoss.multiply(proportion).setScale(2, RoundingMode.HALF_UP)

// Recalcula ROI baseado nos valores proporcionais
val proportionalRoi = calculateRoi(proportionalProfit, proportionalStake)
```

---

## 🧮 Exemplo Prático

### Cenário 1: Ticket com 3 Mercados

**Dados do Ticket:**
- Stake: R$ 30,00
- Profit: R$ 15,00
- Seleções:
  - 2x "Handicap"
  - 1x "Total de Gols"
  - 0x "1X2"
- **Total**: 3 seleções

**Resultado da Divisão:**

| Mercado | Seleções | Proporção | Stake | Profit | ROI |
|---------|----------|-----------|-------|--------|-----|
| Handicap | 2 | 2/3 = 0.6667 | R$ 20,00 | R$ 10,00 | 50% |
| Total de Gols | 1 | 1/3 = 0.3333 | R$ 10,00 | R$ 5,00 | 50% |
| 1X2 | 0 | 0/3 = 0.0000 | R$ 0,00 | R$ 0,00 | 0% |
| **TOTAL** | **3** | **1.0000** | **R$ 30,00** | **R$ 15,00** | **50%** |

✅ **Soma dos stakes**: R$ 20 + R$ 10 = R$ 30 (correto!)

---

### Cenário 2: Ticket com 5 Mercados

**Dados do Ticket:**
- Stake: R$ 50,00
- Profit: R$ -20,00 (perda)
- Seleções:
  - 2x "Handicap"
  - 2x "Total de Gols"
  - 1x "Ambas Marcam"
- **Total**: 5 seleções

**Resultado da Divisão:**

| Mercado | Seleções | Proporção | Stake | Profit | ROI |
|---------|----------|-----------|-------|--------|-----|
| Handicap | 2 | 2/5 = 0.4000 | R$ 20,00 | R$ -8,00 | -40% |
| Total de Gols | 2 | 2/5 = 0.4000 | R$ 20,00 | R$ -8,00 | -40% |
| Ambas Marcam | 1 | 1/5 = 0.2000 | R$ 10,00 | R$ -4,00 | -40% |
| **TOTAL** | **5** | **1.0000** | **R$ 50,00** | **R$ -20,00** | **-40%** |

✅ **Soma dos stakes**: R$ 20 + R$ 20 + R$ 10 = R$ 50 (correto!)

---

### Cenário 3: Ticket Simples (1 Mercado)

**Dados do Ticket:**
- Stake: R$ 15,00
- Profit: R$ 30,00
- Seleções:
  - 1x "1X2"
- **Total**: 1 seleção

**Resultado da Divisão:**

| Mercado | Seleções | Proporção | Stake | Profit | ROI |
|---------|----------|-----------|-------|--------|-----|
| 1X2 | 1 | 1/1 = 1.0000 | R$ 15,00 | R$ 30,00 | 200% |
| **TOTAL** | **1** | **1.0000** | **R$ 15,00** | **R$ 30,00** | **200%** |

✅ **Soma dos stakes**: R$ 15 = R$ 15 (correto!)

---

## 🔍 Diferença Entre Endpoints

### Por Torneio - Valor COMPLETO
```kotlin
// Cada torneio conta o valor completo do ticket
totalStake = event.stake           // R$ 30 completo
totalProfit = event.profitLoss     // R$ 15 completo
roi = event.roi                     // 50%
```

**Motivo:** Mostra o "impacto de cada torneio nos tickets"

### Por Mercado - Valor PROPORCIONAL
```kotlin
// Cada mercado conta proporcionalmente
val proportion = selectionsInMarket / totalSelections
totalStake = event.stake × proportion      // R$ 20 (2/3)
totalProfit = event.profitLoss × proportion // R$ 10 (2/3)
roi = calculateRoi(totalProfit, totalStake) // 50%
```

**Motivo:** Mostra "quanto estou investindo em cada mercado"

---

## 📝 Código Completo das Alterações

### createNewByMarket() - DEPOIS

```kotlin
private fun createNewByMarket(id: PerformanceByMarketId, event: TicketSettledEvent): PerformanceByMarketEntity {
    val ticketCounts = countTicketsByStatus(event)
    val selectionCounts = countSelectionsByStatus(event, id.marketType)
    val selectionsInMarket = event.selections.count { it.marketType == id.marketType }

    // ✅ NOVO: Calcula divisão proporcional
    val totalSelections = event.selections.size
    val proportion = if (totalSelections > 0) {
        BigDecimal(selectionsInMarket).divide(BigDecimal(totalSelections), 4, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ONE
    }

    // ✅ NOVO: Aplica proporção aos valores financeiros
    val proportionalStake = event.stake.multiply(proportion).setScale(2, RoundingMode.HALF_UP)
    val proportionalProfit = event.profitLoss.multiply(proportion).setScale(2, RoundingMode.HALF_UP)
    val proportionalRoi = calculateRoi(proportionalProfit, proportionalStake)

    return PerformanceByMarketEntity(
        id = id,
        totalSelections = selectionsInMarket,
        wins = selectionCounts.wins,
        losses = selectionCounts.losses,
        voids = selectionCounts.voids,
        uniqueTickets = 1,
        ticketsFullWon = ticketCounts.fullWon,
        ticketsPartialWon = ticketCounts.partialWon,
        ticketsBreakEven = ticketCounts.breakEven,
        ticketsPartialLost = ticketCounts.partialLost,
        ticketsTotalLost = ticketCounts.totalLosses,
        // ✅ ALTERADO: Usa valores proporcionais
        totalStake = proportionalStake,
        totalProfit = proportionalProfit,
        roi = proportionalRoi,
        winRate = calculateWinRate(selectionCounts.wins, selectionsInMarket),
        successRate = calculateWinRate(ticketCounts.fullWon + ticketCounts.partialWon, 1),
        avgOdd = event.totalOdd,
        firstBetAt = event.settledAt,
        lastSettledAt = event.settledAt,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
```

### updateExistingByMarket() - DEPOIS

```kotlin
private fun updateExistingByMarket(entity: PerformanceByMarketEntity, event: TicketSettledEvent) {
    val ticketCounts = countTicketsByStatus(event)
    val selectionCounts = countSelectionsByStatus(event, entity.id.marketType)
    val selectionsInMarket = event.selections.count { it.marketType == entity.id.marketType }

    // ✅ NOVO: Calcula divisão proporcional
    val totalSelections = event.selections.size
    val proportion = if (totalSelections > 0) {
        BigDecimal(selectionsInMarket).divide(BigDecimal(totalSelections), 4, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ONE
    }

    // ✅ NOVO: Aplica proporção aos valores financeiros
    val proportionalStake = event.stake.multiply(proportion).setScale(2, RoundingMode.HALF_UP)
    val proportionalProfit = event.profitLoss.multiply(proportion).setScale(2, RoundingMode.HALF_UP)

    entity.totalSelections += selectionsInMarket
    entity.uniqueTickets++

    entity.wins += selectionCounts.wins
    entity.losses += selectionCounts.losses
    entity.voids += selectionCounts.voids

    entity.ticketsFullWon += ticketCounts.fullWon
    entity.ticketsPartialWon += ticketCounts.partialWon
    entity.ticketsBreakEven += ticketCounts.breakEven
    entity.ticketsPartialLost += ticketCounts.partialLost
    entity.ticketsTotalLost += ticketCounts.aggregatedLosses

    // ✅ ALTERADO: Adiciona valores proporcionais
    entity.totalStake += proportionalStake
    entity.totalProfit += proportionalProfit
    entity.roi = calculateRoi(entity.totalProfit, entity.totalStake)

    entity.winRate = calculateWinRate(entity.wins, entity.totalSelections)
    entity.successRate = calculateWinRate(entity.ticketsFullWon + entity.ticketsPartialWon, entity.uniqueTickets)
    entity.avgOdd = calculateIncrementalAvg(entity.avgOdd, entity.uniqueTickets - 1, event.totalOdd, entity.uniqueTickets)
    entity.lastSettledAt = event.settledAt
    entity.updatedAt = System.currentTimeMillis()
}
```

---

## ✅ Benefícios da Correção

### 1. Dados Financeiros Precisos
- Soma dos stakes por mercado = stake total investido ✅
- Não há mais inflação de valores

### 2. Análise Mais Realista
- Mostra quanto foi investido em cada tipo de mercado
- Permite identificar mercados mais rentáveis

### 3. Comparação Justa
- Mercados com mais seleções não são artificialmente inflados
- ROI de cada mercado reflete proporção real

---

## 🧪 Validação Necessária

Após recriar o banco e importar tickets:

### Teste 1: Ticket com Múltiplos Mercados
```sql
-- Verificar soma dos stakes por mercado
SELECT
    SUM(total_stake) as soma_stakes_mercados
FROM analytics.performance_by_market
WHERE user_id = 1;

-- Comparar com overall
SELECT
    total_stake as stake_overall
FROM analytics.performance_overall
WHERE user_id = 1;

-- ✅ EXPECTATIVA: soma_stakes_mercados ≈ stake_overall
```

### Teste 2: Ticket Simples (1 Mercado)
```sql
-- Ticket com 1 seleção deve ter stake completo no mercado
SELECT
    market_type,
    total_stake,
    total_profit,
    roi
FROM analytics.performance_by_market
WHERE user_id = 1
  AND unique_tickets = 1
  AND total_selections = 1;

-- ✅ EXPECTATIVA: total_stake = valor completo do ticket
```

### Teste 3: Proporções Corretas
```sql
-- Para um ticket com 3 seleções em 2 mercados diferentes
-- Verificar se as proporções estão corretas
SELECT
    market_type,
    total_selections,
    total_stake,
    ROUND(total_stake / (SELECT total_stake FROM analytics.performance_overall WHERE user_id = 1) * 100, 2) as percentual
FROM analytics.performance_by_market
WHERE user_id = 1;

-- ✅ EXPECTATIVA: soma dos percentuais ≈ 100%
```

---

## 📊 Impacto nos Endpoints

### GET /api/analytics/by-market

**Response (exemplo com ticket de R$ 30 e 3 mercados):**

```json
[
  {
    "marketType": "Handicap",
    "totalSelections": 2,
    "uniqueTickets": 1,
    "totalStaked": 20.00,  // ✅ 2/3 de R$ 30
    "profitLoss": 10.00,   // ✅ 2/3 de R$ 15
    "roi": 50.00
  },
  {
    "marketType": "Total de Gols",
    "totalSelections": 1,
    "uniqueTickets": 1,
    "totalStaked": 10.00,  // ✅ 1/3 de R$ 30
    "profitLoss": 5.00,    // ✅ 1/3 de R$ 15
    "roi": 50.00
  }
]
```

**Soma:** R$ 20 + R$ 10 = **R$ 30** ✅

---

## 🎯 Status Final

| Correção | Status |
|----------|--------|
| Divisão proporcional implementada | ✅ COMPLETO |
| Método `createNewByMarket()` | ✅ CORRIGIDO |
| Método `updateExistingByMarket()` | ✅ CORRIGIDO |
| Testes unitários | ⬜ PENDENTE |
| Validação em banco real | ⬜ PENDENTE |

---

## 🚀 Próximos Passos

1. ⬜ Criar testes unitários para validar a divisão proporcional
2. ⬜ Recriar banco de dados
3. ⬜ Importar tickets de teste
4. ⬜ Executar queries de validação
5. ⬜ Validar endpoint `/api/analytics/by-market`
