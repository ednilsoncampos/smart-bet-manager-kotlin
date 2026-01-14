package com.smartbet.application.dto

/**
 * Representa um ponto de dados na evolução do saldo
 */
data class BankrollEvolutionPoint(
    val date: Long,           // Timestamp em milissegundos
    val balance: Double,      // Saldo naquele momento
    val profitLoss: Double,   // Lucro/Prejuízo acumulado
    val roi: Double           // ROI até aquele momento
)

/**
 * Resposta com a evolução do saldo de uma banca específica
 */
data class BankrollEvolutionResponse(
    val bankrollId: Long,
    val bankrollName: String,
    val providerName: String?,
    val currency: String,
    val currentBalance: Double,
    val totalProfitLoss: Double,
    val overallRoi: Double,
    val evolution: List<BankrollEvolutionPoint>
)

/**
 * Resposta com a evolução consolidada de todas as bancas
 */
data class ConsolidatedEvolutionResponse(
    val totalCurrentBalance: Double,
    val totalProfitLoss: Double,
    val overallRoi: Double,
    val evolution: List<BankrollEvolutionPoint>,
    val byProvider: List<ProviderEvolutionSummary>
)

/**
 * Resumo de evolução por casa de apostas
 */
data class ProviderEvolutionSummary(
    val providerId: Long,
    val providerName: String,
    val currentBalance: Double,
    val profitLoss: Double,
    val roi: Double
)

/**
 * Parâmetros para consulta de evolução
 */
data class EvolutionQueryParams(
    val startDate: Long? = null,    // Timestamp inicial (opcional)
    val endDate: Long? = null,      // Timestamp final (opcional)
    val granularity: String = "day" // day, week, month
)
