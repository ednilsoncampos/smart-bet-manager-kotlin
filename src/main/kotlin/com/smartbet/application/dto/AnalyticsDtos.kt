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
    val averageOdd: BigDecimal,
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
    val wins: Long,
    val losses: Long,
    val winRate: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal
)
