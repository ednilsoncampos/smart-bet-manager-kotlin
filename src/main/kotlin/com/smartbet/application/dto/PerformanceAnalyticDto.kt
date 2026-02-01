package com.smartbet.application.dto

import java.math.BigDecimal

// ============================================
// Response DTOs para Performance Analytics
// ============================================

data class OverallPerformanceResponse(
    val totalBets: Long,

    // Contadores agregados (compatibilidade retroativa)
    /** Total de vitórias (full_won + partial_won) - compatibilidade */
    val wins: Long,
    /** Total de derrotas (partial_lost + total_lost) - compatibilidade */
    val losses: Long,
    /** Tickets anulados/void (break_even) - compatibilidade */
    val voids: Long,
    /** Tickets com cashout */
    val cashedOut: Long,

    // Contadores granulares por FinancialStatus
    /** Vitórias completas (FULL_WIN): retorno >= potencial máximo */
    val fullWins: Long,
    /** Vitórias parciais (PARTIAL_WIN): sistema parcial ou cashout com lucro */
    val partialWins: Long,
    /** Empates (BREAK_EVEN): retorno = stake (anulação, cashout exato, ou sistema) */
    val breakEven: Long,
    /** Perdas parciais (PARTIAL_LOSS): comum em sistemas, ou cashout com prejuízo */
    val partialLosses: Long,
    /** Perdas totais (TOTAL_LOSS): retorno = 0 */
    val totalLosses: Long,

    // Métricas
    /** Taxa de acerto pura - apenas vitórias completas (FULL_WIN) em % */
    val winRate: BigDecimal,
    /** Taxa de sucesso - vitórias totais + parciais (FULL_WIN + PARTIAL_WIN) incluindo sistemas e cashouts em % */
    val successRate: BigDecimal,
    val totalStaked: BigDecimal,
    val totalReturns: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds dos tickets */
    val avgOdd: BigDecimal?,
    val avgStake: BigDecimal?,

    // Métricas granulares derivadas
    /** Taxa de acerto completo (full_wins / total_bets * 100) */
    val fullWinRate: BigDecimal,
    /** Taxa de acerto parcial (partial_wins / total_bets * 100) */
    val partialWinRate: BigDecimal,
    /** Taxa de empate (break_even / total_bets * 100) */
    val breakEvenRate: BigDecimal,
    /** Taxa de perda parcial (partial_losses / total_bets * 100) */
    val partialLossRate: BigDecimal,
    /** Taxa de perda total (total_losses / total_bets * 100) */
    val totalLossRate: BigDecimal,
    /** Taxa de sucesso do cashout (partial_wins / (partial_wins + partial_losses) * 100), null se nenhum cashout */
    val cashoutSuccessRate: BigDecimal?,

    // Gamificação: Streaks
    /** Sequência atual: >0 = vitórias seguidas, <0 = derrotas seguidas, 0 = sem sequência */
    val currentStreak: Int,
    /** Melhor sequência de vitórias */
    val bestWinStreak: Int,
    /** Pior sequência de derrotas (valor negativo) */
    val worstLossStreak: Int,

    // Gamificação: Records
    /** Maior vitória individual */
    val biggestWin: BigDecimal?,
    /** Maior perda individual (valor negativo) */
    val biggestLoss: BigDecimal?,
    /** Melhor ROI de um ticket */
    val bestRoiTicket: BigDecimal?,

    // Timestamps
    val firstBetAt: Long?,
    val lastSettledAt: Long
)

data class PerformanceByMonthResponse(
    val year: Int,
    val month: Int,
    val totalBets: Long,

    // Contadores agregados (compatibilidade)
    /** Total de vitórias (full_won + partial_won) */
    val wins: Long,
    /** Total de derrotas (partial_lost + total_lost) */
    val losses: Long,
    /** Tickets anulados/void (break_even) */
    val voids: Long,

    // Contadores granulares
    val fullWins: Long,
    val partialWins: Long,
    val breakEven: Long,
    val partialLosses: Long,
    val totalLosses: Long,

    // Métricas
    /** Taxa de acerto pura - apenas vitórias completas (FULL_WIN) em % */
    val winRate: BigDecimal,
    /** Taxa de sucesso - vitórias totais + parciais (FULL_WIN + PARTIAL_WIN) em % */
    val successRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    val avgStake: BigDecimal?,

    // Métricas granulares
    val fullWinRate: BigDecimal,
    val partialWinRate: BigDecimal,
    val partialLossRate: BigDecimal,
    val totalLossRate: BigDecimal,

    // Timestamps
    val firstBetAt: Long?,
    val lastSettledAt: Long
)

data class PerformanceByTournamentResponse(
    val tournamentId: Long,
    val tournamentName: String,
    /** Nome local/país do torneio */
    val tournamentLocalName: String? = null,
    val totalBets: Long,

    // Contadores agregados (compatibilidade)
    /** Total de vitórias (full_won + partial_won) */
    val wins: Long,
    /** Total de derrotas (partial_lost + total_lost) */
    val losses: Long,
    /** Tickets anulados/void (break_even) */
    val voids: Long,

    // Contadores granulares
    val fullWins: Long,
    val partialWins: Long,
    val breakEven: Long,
    val partialLosses: Long,
    val totalLosses: Long,

    // Métricas
    /** Taxa de acerto pura - apenas vitórias completas (FULL_WIN) em % */
    val winRate: BigDecimal,
    /** Taxa de sucesso - vitórias totais + parciais (FULL_WIN + PARTIAL_WIN) em % */
    val successRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds dos tickets neste torneio */
    val avgOdd: BigDecimal?,

    // Métricas granulares
    val fullWinRate: BigDecimal,
    val partialWinRate: BigDecimal,
    val partialLossRate: BigDecimal,
    val totalLossRate: BigDecimal,

    // Timestamps
    val firstBetAt: Long?,
    val lastSettledAt: Long
)

data class PerformanceByMarketResponse(
    val marketType: String,
    /** Total de seleções neste mercado (uma aposta múltipla conta N vezes) */
    val totalSelections: Long,
    /** Número de tickets únicos que incluem esse mercado */
    val uniqueTickets: Long,

    // Contadores agregados (baseados em seleções - compatibilidade)
    /** Seleções ganhas neste mercado */
    val wins: Long,
    /** Seleções perdidas neste mercado */
    val losses: Long,
    /** Seleções anuladas neste mercado */
    val voids: Long,

    // Contadores granulares (baseados no FinancialStatus dos tickets)
    val fullWins: Long,
    val partialWins: Long,
    val breakEven: Long,
    val partialLosses: Long,
    val totalLosses: Long,

    // Métricas
    /** Taxa de acerto pura - apenas vitórias completas (FULL_WIN) em % */
    val winRate: BigDecimal,
    /** Taxa de sucesso - vitórias totais + parciais (FULL_WIN + PARTIAL_WIN) em % */
    val successRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds das seleções neste mercado */
    val avgOdd: BigDecimal?,

    // Métricas granulares
    val fullWinRate: BigDecimal,
    val partialWinRate: BigDecimal,
    val partialLossRate: BigDecimal,
    val totalLossRate: BigDecimal,

    // Timestamps
    val firstBetAt: Long?,
    val lastSettledAt: Long,

    /** Componentes individuais do Bet Builder (apenas quando marketType = "Criar Aposta") */
    val betBuilderComponents: List<BetBuilderComponentStats>? = null
)

/**
 * Estatísticas de um componente individual do Bet Builder.
 * Agrupa por evento + mercado + seleção (ex: "Flamengo vs Palmeiras", "Ambas Marcam", "Sim").
 */
data class BetBuilderComponentStats(
    /** Nome do evento/jogo (ex: "Flamengo vs Palmeiras") */
    val eventName: String,
    /** Nome do mercado do componente */
    val marketName: String,
    /** Nome da seleção do componente */
    val selectionName: String,
    /** Total de apostas com este componente */
    val totalBets: Long,
    /** Vitórias (componente com status WON) */
    val wins: Long,
    /** Derrotas (componente com status LOST) */
    val losses: Long,
    /** Taxa de acerto do componente */
    val winRate: BigDecimal
)

data class PerformanceByProviderResponse(
    val providerId: Long,
    val providerName: String,
    val totalBets: Long,

    // Contadores agregados (compatibilidade)
    /** Total de vitórias (full_won + partial_won) */
    val wins: Long,
    /** Total de derrotas (partial_lost + total_lost) */
    val losses: Long,
    /** Tickets anulados/void (break_even) */
    val voids: Long,
    /** Tickets com cashout */
    val cashedOut: Long,

    // Contadores granulares
    val fullWins: Long,
    val partialWins: Long,
    val breakEven: Long,
    val partialLosses: Long,
    val totalLosses: Long,

    // Métricas
    /** Taxa de acerto pura - apenas vitórias completas (FULL_WIN) em % */
    val winRate: BigDecimal,
    /** Taxa de sucesso - vitórias totais + parciais (FULL_WIN + PARTIAL_WIN) em % */
    val successRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds dos tickets */
    val avgOdd: BigDecimal?,
    /** Média do valor apostado por ticket */
    val avgStake: BigDecimal?,

    // Métricas granulares
    val fullWinRate: BigDecimal,
    val partialWinRate: BigDecimal,
    val partialLossRate: BigDecimal,
    val totalLossRate: BigDecimal,
    val cashoutSuccessRate: BigDecimal?,

    // Timestamps
    val firstBetAt: Long?,
    val lastSettledAt: Long
)
