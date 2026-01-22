package com.smartbet.application.dto

import java.math.BigDecimal

// ============================================
// Response DTOs
// ============================================

data class OverallPerformanceResponse(
    val totalBets: Long,
    val settledBets: Long,
    val openBets: Long,
    val wins: Long,
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
    val totalBets: Long,
    val wins: Long,
    val losses: Long,
    val winRate: BigDecimal
)

data class PerformanceByMarketResponse(
    val marketType: String,
    val totalBets: Long,
    val wins: Long,
    val losses: Long,
    val winRate: BigDecimal
)

data class PerformanceByProviderResponse(
    val providerId: Long,
    val providerName: String,
    val totalBets: Long,
    /** Total de vitórias (FULL_WIN + PARTIAL_WIN) */
    val wins: Long,
    /** Total de derrotas (TOTAL_LOSS + PARTIAL_LOSS) */
    val losses: Long,
    /** Vitórias totais (sem erros) */
    val fullWins: Long,
    /** Vitórias parciais (com erros mas lucro) */
    val partialWins: Long,
    /** Derrotas totais */
    val totalLosses: Long,
    /** Derrotas parciais (com acertos mas prejuízo) */
    val partialLosses: Long,
    /** Empates (stake = payout) */
    val breakEven: Long,
    val winRate: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal
)
