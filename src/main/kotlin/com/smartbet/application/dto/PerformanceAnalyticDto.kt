package com.smartbet.application.dto

import java.math.BigDecimal

// ============================================
// Response DTOs para Performance Analytics
// ============================================

data class OverallPerformanceResponse(
    val totalBets: Long,
    val settledBets: Long,
    val openBets: Long,
    /** Vitórias totais (sem erros) */
    val fullWins: Long,
    /** Vitórias parciais (com erros mas lucro) */
    val partialWins: Long,
    /** Empates (stake = payout) */
    val breakEven: Long,
    /** Derrotas parciais (com acertos mas prejuízo) */
    val partialLosses: Long,
    /** Derrotas totais */
    val totalLosses: Long,
    /** Total de vitórias (FULL_WIN + PARTIAL_WIN) */
    val wins: Long,
    /** Total de derrotas (TOTAL_LOSS + PARTIAL_LOSS) */
    val losses: Long,
    val winRate: BigDecimal,
    val totalStaked: BigDecimal,
    val totalReturns: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    /** Mediana das odds (mais resistente a outliers que a média) */
    val medianOdd: BigDecimal,
    val averageStake: BigDecimal
)

data class PerformanceByTournamentResponse(
    val tournamentName: String,
    /** Nome local/país do torneio */
    val tournamentLocalName: String? = null,
    val totalBets: Long,
    /** Vitórias totais (sem erros) */
    val fullWins: Long,
    /** Vitórias parciais (com erros mas lucro) */
    val partialWins: Long,
    /** Empates (stake = payout) */
    val breakEven: Long,
    /** Derrotas parciais (com acertos mas prejuízo) */
    val partialLosses: Long,
    /** Derrotas totais */
    val totalLosses: Long,
    /** Total de vitórias (FULL_WIN + PARTIAL_WIN) */
    val wins: Long,
    /** Total de derrotas (TOTAL_LOSS + PARTIAL_LOSS) */
    val losses: Long,
    val winRate: BigDecimal
)

data class PerformanceByMarketResponse(
    val marketType: String,
    val totalBets: Long,
    /** Vitórias totais (sem erros) */
    val fullWins: Long,
    /** Vitórias parciais (com erros mas lucro) */
    val partialWins: Long,
    /** Empates (stake = payout) */
    val breakEven: Long,
    /** Derrotas parciais (com acertos mas prejuízo) */
    val partialLosses: Long,
    /** Derrotas totais */
    val totalLosses: Long,
    /** Total de vitórias (FULL_WIN + PARTIAL_WIN) */
    val wins: Long,
    /** Total de derrotas (TOTAL_LOSS + PARTIAL_LOSS) */
    val losses: Long,
    val winRate: BigDecimal,
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
    /** Vitórias totais (sem erros) */
    val fullWins: Long,
    /** Vitórias parciais (com erros mas lucro) */
    val partialWins: Long,
    /** Empates (stake = payout) */
    val breakEven: Long,
    /** Derrotas parciais (com acertos mas prejuízo) */
    val partialLosses: Long,
    /** Derrotas totais */
    val totalLosses: Long,
    /** Total de vitórias (FULL_WIN + PARTIAL_WIN) */
    val wins: Long,
    /** Total de derrotas (TOTAL_LOSS + PARTIAL_LOSS) */
    val losses: Long,
    val winRate: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal
)
