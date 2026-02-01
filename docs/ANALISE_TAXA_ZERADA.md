# Análise: Taxa Zerada na Performance por Torneio

## Problema Identificado

Na tela "Análise de Performance > Por Torneio", todos os torneios estão mostrando **0% Taxa**, mesmo quando há acertos registrados e ROI calculado corretamente.

### Exemplos da tela:
- **Bundesliga**: 1 Apostas, 1 Acertos, 0 Erros, **0% Taxa**, +355.9% ROI
  - Esperado: Taxa = 100% (1/1 * 100)
- **Série A**: 2 Apostas, 1 Acertos, 1 Erros, **0% Taxa**, +85.2% ROI
  - Esperado: Taxa = 50% (1/2 * 100)
- **Copa**: 2 Apostas, 1 Acertos, 1 Erros, **0% Taxa**, +85.2% ROI
  - Esperado: Taxa = 50% (1/2 * 100)

## Endpoint Afetado

**GET /api/analytics/by-tournament**

Response: `PerformanceByTournamentResponse`

Campos disponíveis:
- `winRate`: Taxa de acerto pura - apenas vitórias completas (FULL_WIN) em %
- `successRate`: Taxa de sucesso - vitórias totais + parciais (FULL_WIN + PARTIAL_WIN) em %
- `roi`: ROI (retorno sobre investimento) - **ESTÁ FUNCIONANDO**

## Análise do Código

### 1. Service Layer (`PerformanceAnalyticService.kt:123-157`)

O método `getPerformanceByTournament()` mapeia os dados diretamente da entidade:

```kotlin
PerformanceByTournamentResponse(
    winRate = performance.winRate,           // <-- Pega direto da entidade
    successRate = performance.successRate,   // <-- Pega direto da entidade
    roi = performance.roi,                   // <-- Pega direto da entidade (funciona!)
    fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
    partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
    // ...
)
```

**Observação**: O `roi` está funcionando corretamente, mas `winRate` e `successRate` estão zerados.

### 2. Aggregation Layer (`AnalyticsAggregationService.kt`)

#### createNewByTournament (linha 311-337)
```kotlin
return PerformanceByTournamentEntity(
    // ...
    roi = event.roi,                                           // Copia do evento
    winRate = calculateWinRate(counts.fullWon, 1),            // Calcula para primeiro ticket
    successRate = calculateWinRate(counts.aggregatedWins, 1), // Calcula para primeiro ticket
    // ...
)
```

#### updateExistingByTournament (linha 462-486)
```kotlin
entity.roi = calculateRoi(entity.totalProfit, entity.totalStake)
entity.winRate = calculateWinRate(entity.ticketsFullWon, entity.totalTickets)
entity.successRate = calculateWinRate(entity.ticketsWon, entity.totalTickets)
```

#### calculateWinRate (linha 620-626)
```kotlin
private fun calculateWinRate(wins: Int, total: Int): BigDecimal {
    if (total == 0) return BigDecimal.ZERO
    return BigDecimal(wins)
        .divide(BigDecimal(total), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .setScale(2, RoundingMode.HALF_UP)
}
```

**O método de cálculo está correto!**

### 3. Database Schema

```sql
win_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
success_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
```

**Schema está correto.** Suporta valores de 0.00 a 999.99.

## Possíveis Causas

### Hipótese 1: Dados Antigos Zerados ✅ MAIS PROVÁVEL
Os dados foram importados ou migrados ANTES da implementação correta do cálculo de `winRate` e `successRate`, resultando em valores zerados no banco de dados.

**Evidência**: O ROI está calculado corretamente, o que sugere que o código atual funciona, mas os dados históricos estão incorretos.

### Hipótese 2: Frontend Pegando Campo Errado
O frontend pode estar renderizando um campo diferente ou fazendo um cálculo incorreto.

**Verificar**: Qual campo exatamente o frontend está usando para "Taxa"?

### Hipótese 3: Problema na Ordem de Execução
Os valores podem estar sendo calculados incorretamente devido à ordem das operações, mas isso é IMPROVÁVEL dado que o ROI funciona.

## Soluções Propostas

### Solução 1: Recalcular Dados Existentes (Script de Migração)

Criar um script SQL para recalcular `win_rate` e `success_rate` baseado nos dados existentes:

```sql
UPDATE analytics.performance_by_tournament
SET
    win_rate = CASE
        WHEN total_tickets > 0 THEN
            ROUND((tickets_full_won::DECIMAL / total_tickets::DECIMAL) * 100, 2)
        ELSE 0
    END,
    success_rate = CASE
        WHEN total_tickets > 0 THEN
            ROUND((tickets_won::DECIMAL / total_tickets::DECIMAL) * 100, 2)
        ELSE 0
    END,
    updated_at = FLOOR(EXTRACT(EPOCH FROM NOW()) * 1000)
WHERE win_rate = 0 OR success_rate = 0;
```

### Solução 2: Adicionar Endpoint de Debug

Criar um endpoint temporário para verificar os dados brutos:

```kotlin
@GetMapping("/analytics/by-tournament/debug")
fun debugPerformanceByTournament(@AuthenticationPrincipal userId: Long): ResponseEntity<Map<String, Any>> {
    val performances = byTournamentRepository.findByIdUserId(userId)
    return ResponseEntity.ok(mapOf(
        "count" to performances.size,
        "data" to performances.map {
            mapOf(
                "tournamentId" to it.id.tournamentId,
                "totalTickets" to it.totalTickets,
                "ticketsWon" to it.ticketsWon,
                "ticketsFullWon" to it.ticketsFullWon,
                "winRate" to it.winRate,
                "successRate" to it.successRate,
                "roi" to it.roi
            )
        }
    ))
}
```

### Solução 3: Forçar Recalculo no Service

Modificar o `PerformanceAnalyticService.getPerformanceByTournament()` para recalcular as taxas em vez de usar os valores armazenados:

```kotlin
PerformanceByTournamentResponse(
    // ...
    winRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
    successRate = calculateRate(performance.ticketsWon, performance.totalTickets),
    // ...
)
```

Onde `calculateRate` é o método existente:
```kotlin
private fun calculateRate(count: Int, total: Int): BigDecimal {
    if (total == 0) return BigDecimal.ZERO
    return BigDecimal(count)
        .divide(BigDecimal(total), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .setScale(2, RoundingMode.HALF_UP)
}
```

## Recomendação

**Implementar Solução 3 primeiro** (correção no service) para garantir que o endpoint retorne valores corretos imediatamente, independentemente do que está no banco de dados.

Depois, **implementar Solução 1** (script de migração) para corrigir os dados históricos e manter consistência.

## Próximos Passos

1. ✅ Executar script SQL de debug (`debug_tournament_performance.sql`) para verificar dados no banco
2. ⬜ Implementar Solução 3 no `PerformanceAnalyticService.kt`
3. ⬜ Testar o endpoint `/api/analytics/by-tournament`
4. ⬜ Executar script de migração (Solução 1) se necessário
5. ⬜ Verificar se outros endpoints de analytics têm o mesmo problema
