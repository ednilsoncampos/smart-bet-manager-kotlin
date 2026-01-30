package com.smartbet.application.dto

import java.math.BigDecimal

// ============================================
// Response DTOs para Performance Analytics
// ============================================

data class OverallPerformanceResponse(
    val totalBets: Long,
    /** Total de vitórias (tickets ganhos) */
    val wins: Long,
    /** Total de derrotas (tickets perdidos) */
    val losses: Long,
    /** Tickets anulados/void */
    val voids: Long,
    /** Tickets com cashout */
    val cashedOut: Long,
    val winRate: BigDecimal,
    val totalStaked: BigDecimal,
    val totalReturns: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds dos tickets */
    val avgOdd: BigDecimal?,
    val avgStake: BigDecimal?,

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
    /** Total de vitórias (tickets ganhos) */
    val wins: Long,
    /** Total de derrotas (tickets perdidos) */
    val losses: Long,
    /** Tickets anulados/void */
    val voids: Long,
    val winRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    val avgStake: BigDecimal?,

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
    /** Total de vitórias (tickets ganhos) */
    val wins: Long,
    /** Total de derrotas (tickets perdidos) */
    val losses: Long,
    /** Tickets anulados/void */
    val voids: Long,
    val winRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds dos tickets neste torneio */
    val avgOdd: BigDecimal?,

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
    /** Seleções ganhas neste mercado */
    val wins: Long,
    /** Seleções perdidas neste mercado */
    val losses: Long,
    /** Seleções anuladas neste mercado */
    val voids: Long,
    val winRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds das seleções neste mercado */
    val avgOdd: BigDecimal?,

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
    /** Total de vitórias (tickets ganhos) */
    val wins: Long,
    /** Total de derrotas (tickets perdidos) */
    val losses: Long,
    /** Tickets anulados/void */
    val voids: Long,
    /** Tickets com cashout */
    val cashedOut: Long,
    val winRate: BigDecimal,
    val totalStaked: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Média das odds dos tickets */
    val avgOdd: BigDecimal?,

    // Timestamps
    val firstBetAt: Long?,
    val lastSettledAt: Long
)
