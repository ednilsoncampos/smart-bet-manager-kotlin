package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.entity.BetSelectionEntity
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
import com.smartbet.infrastructure.persistence.repository.*
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Service de analytics que utiliza as tabelas pré-agregadas do schema analytics.
 *
 * Este service lê dados das tabelas analytics.performance_* que são atualizadas
 * incrementalmente via eventos pelo AnalyticsAggregationService.
 */
@Service
class PerformanceAnalyticService(
    private val overallRepository: PerformanceOverallRepository,
    private val byProviderRepository: PerformanceByProviderRepository,
    private val byMarketRepository: PerformanceByMarketRepository,
    private val byMonthRepository: PerformanceByMonthRepository,
    private val byTournamentRepository: PerformanceByTournamentRepository,
    private val providerRepository: BettingProviderRepository,
    private val tournamentRepository: TournamentRepository,
    private val selectionComponentRepository: BetSelectionComponentRepository,
    private val ticketRepository: BetTicketRepository
) {
    
    /**
     * Retorna a performance geral do usuário usando a tabela analytics.performance_overall.
     */
    fun getOverallPerformance(userId: Long): OverallPerformanceResponse {
        val performance = overallRepository.findByUserId(userId)
            ?: return createEmptyOverallResponse()

        return OverallPerformanceResponse(
            totalBets = performance.totalTickets.toLong(),
            // Contadores agregados (compatibilidade)
            wins = performance.ticketsWon.toLong(),
            losses = performance.ticketsLost.toLong(),
            voids = performance.ticketsVoid.toLong(),
            cashedOut = performance.ticketsCashedOut.toLong(),
            // Contadores granulares
            fullWins = performance.ticketsFullWon.toLong(),
            partialWins = performance.ticketsPartialWon.toLong(),
            breakEven = performance.ticketsBreakEven.toLong(),
            partialLosses = performance.ticketsPartialLost.toLong(),
            totalLosses = performance.ticketsTotalLost.toLong(),
            // Métricas
            winRate = performance.winRate,
            totalStaked = performance.totalStake,
            totalReturns = performance.totalReturn,
            profitLoss = performance.totalProfit,
            roi = performance.roi,
            avgOdd = performance.avgOdd,
            avgStake = performance.avgStake,
            // Métricas granulares
            fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
            partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
            breakEvenRate = calculateRate(performance.ticketsBreakEven, performance.totalTickets),
            cashoutSuccessRate = calculateCashoutSuccessRate(
                performance.ticketsPartialWon,
                performance.ticketsPartialLost
            ),
            // Gamificação
            currentStreak = performance.currentStreak,
            bestWinStreak = performance.bestWinStreak,
            worstLossStreak = performance.worstLossStreak,
            biggestWin = performance.biggestWin,
            biggestLoss = performance.biggestLoss,
            bestRoiTicket = performance.bestRoiTicket,
            firstBetAt = performance.firstBetAt,
            lastSettledAt = performance.lastSettledAt
        )
    }

    private fun createEmptyOverallResponse() = OverallPerformanceResponse(
        totalBets = 0,
        wins = 0,
        losses = 0,
        voids = 0,
        cashedOut = 0,
        fullWins = 0,
        partialWins = 0,
        breakEven = 0,
        partialLosses = 0,
        totalLosses = 0,
        winRate = BigDecimal.ZERO,
        totalStaked = BigDecimal.ZERO,
        totalReturns = BigDecimal.ZERO,
        profitLoss = BigDecimal.ZERO,
        roi = BigDecimal.ZERO,
        avgOdd = null,
        avgStake = null,
        fullWinRate = BigDecimal.ZERO,
        partialWinRate = BigDecimal.ZERO,
        breakEvenRate = BigDecimal.ZERO,
        cashoutSuccessRate = null,
        currentStreak = 0,
        bestWinStreak = 0,
        worstLossStreak = 0,
        biggestWin = null,
        biggestLoss = null,
        bestRoiTicket = null,
        firstBetAt = null,
        lastSettledAt = 0
    )
    
    /**
     * Retorna a performance por campeonato/torneio usando a tabela analytics.performance_by_tournament.
     */
    fun getPerformanceByTournament(userId: Long): List<PerformanceByTournamentResponse> {
        val performances = byTournamentRepository.findByIdUserId(userId)
        val tournaments = tournamentRepository.findAll().associateBy { it.id }

        return performances.map { performance ->
            val tournament = tournaments[performance.id.tournamentId]

            PerformanceByTournamentResponse(
                tournamentId = performance.id.tournamentId,
                tournamentName = tournament?.name ?: "Torneio Desconhecido",
                tournamentLocalName = tournament?.localName,
                totalBets = performance.totalTickets.toLong(),
                wins = performance.ticketsWon.toLong(),
                losses = performance.ticketsLost.toLong(),
                voids = performance.ticketsVoid.toLong(),
                fullWins = performance.ticketsFullWon.toLong(),
                partialWins = performance.ticketsPartialWon.toLong(),
                breakEven = performance.ticketsBreakEven.toLong(),
                partialLosses = performance.ticketsPartialLost.toLong(),
                totalLosses = performance.ticketsTotalLost.toLong(),
                winRate = performance.winRate,
                totalStaked = performance.totalStake,
                profitLoss = performance.totalProfit,
                roi = performance.roi,
                avgOdd = performance.avgOdd,
                fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
                partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
                firstBetAt = performance.firstBetAt,
                lastSettledAt = performance.lastSettledAt
            )
        }
    }

    /**
     * Retorna a performance mensal do usuário usando a tabela analytics.performance_by_month.
     * Os dados são retornados ordenados por ano e mês (mais recente primeiro).
     */
    fun getPerformanceByMonth(userId: Long): List<PerformanceByMonthResponse> {
        val performances = byMonthRepository.findByIdUserIdOrderByIdYearDescIdMonthDesc(userId)

        return performances.map { performance ->
            PerformanceByMonthResponse(
                year = performance.id.year,
                month = performance.id.month,
                totalBets = performance.totalTickets.toLong(),
                wins = performance.ticketsWon.toLong(),
                losses = performance.ticketsLost.toLong(),
                voids = performance.ticketsVoid.toLong(),
                fullWins = performance.ticketsFullWon.toLong(),
                partialWins = performance.ticketsPartialWon.toLong(),
                breakEven = performance.ticketsBreakEven.toLong(),
                partialLosses = performance.ticketsPartialLost.toLong(),
                totalLosses = performance.ticketsTotalLost.toLong(),
                winRate = performance.winRate,
                totalStaked = performance.totalStake,
                profitLoss = performance.totalProfit,
                roi = performance.roi,
                avgStake = performance.avgStake,
                fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
                partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
                firstBetAt = performance.firstBetAt,
                lastSettledAt = performance.lastSettledAt
            )
        }
    }

    /**
     * Retorna a performance por tipo de mercado usando a tabela analytics.performance_by_market.
     */
    fun getPerformanceByMarket(userId: Long): List<PerformanceByMarketResponse> {
        val performances = byMarketRepository.findByIdUserId(userId)

        return performances.map { performance ->
            // Se for "Criar Aposta" (Bet Builder), busca os componentes individuais
            val betBuilderStats = if (performance.id.marketType == "Criar Aposta") {
                getBetBuilderComponentStats(userId)
            } else {
                null
            }

            PerformanceByMarketResponse(
                marketType = performance.id.marketType,
                totalSelections = performance.totalSelections.toLong(),
                uniqueTickets = performance.uniqueTickets.toLong(),
                wins = performance.wins.toLong(),
                losses = performance.losses.toLong(),
                voids = performance.voids.toLong(),
                fullWins = performance.ticketsFullWon.toLong(),
                partialWins = performance.ticketsPartialWon.toLong(),
                breakEven = performance.ticketsBreakEven.toLong(),
                partialLosses = performance.ticketsPartialLost.toLong(),
                totalLosses = performance.ticketsTotalLost.toLong(),
                winRate = performance.winRate,
                totalStaked = performance.totalStake,
                profitLoss = performance.totalProfit,
                roi = performance.roi,
                avgOdd = performance.avgOdd,
                fullWinRate = calculateRate(performance.ticketsFullWon, performance.uniqueTickets),
                partialWinRate = calculateRate(performance.ticketsPartialWon, performance.uniqueTickets),
                firstBetAt = performance.firstBetAt,
                lastSettledAt = performance.lastSettledAt,
                betBuilderComponents = betBuilderStats
            )
        }
    }

    /**
     * Obtém estatísticas detalhadas dos componentes de Bet Builder.
     */
    private fun getBetBuilderComponentStats(userId: Long): List<BetBuilderComponentStats> {
        val betBuilderComponents = selectionComponentRepository.findByUserId(userId)

        // Agrupa por eventName + marketName + selectionName
        return betBuilderComponents
            .groupBy { component ->
                val eventName = component.selection?.eventName ?: "Evento Desconhecido"
                "${eventName}||${component.marketName}||${component.selectionName}"
            }
            .map { (key, components) ->
                val parts = key.split("||")
                val compEventName = parts[0]
                val compMarketName = parts[1]
                val compSelectionName = parts[2]

                val compTotal = components.size.toLong()
                val compWins = components.count { it.status == SelectionStatus.WON }.toLong()
                val compLosses = components.count { it.status == SelectionStatus.LOST }.toLong()
                val compWinRate = if (compTotal > 0) {
                    BigDecimal.valueOf(compWins)
                        .divide(BigDecimal.valueOf(compTotal), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                } else {
                    BigDecimal.ZERO
                }

                BetBuilderComponentStats(
                    eventName = compEventName,
                    marketName = compMarketName,
                    selectionName = compSelectionName,
                    totalBets = compTotal,
                    wins = compWins,
                    losses = compLosses,
                    winRate = compWinRate
                )
            }
            .sortedByDescending { it.totalBets }
    }
    
    /**
     * Retorna a performance por casa de apostas usando a tabela analytics.performance_by_provider.
     */
    fun getPerformanceByProvider(userId: Long): List<PerformanceByProviderResponse> {
        val performances = byProviderRepository.findByIdUserId(userId)
        val providers = providerRepository.findAll().associateBy { it.id }

        return performances.map { performance ->
            val provider = providers[performance.id.providerId]

            PerformanceByProviderResponse(
                providerId = performance.id.providerId,
                providerName = provider?.name ?: "Desconhecido",
                totalBets = performance.totalTickets.toLong(),
                wins = performance.ticketsWon.toLong(),
                losses = performance.ticketsLost.toLong(),
                voids = performance.ticketsVoid.toLong(),
                cashedOut = performance.ticketsCashedOut.toLong(),
                fullWins = performance.ticketsFullWon.toLong(),
                partialWins = performance.ticketsPartialWon.toLong(),
                breakEven = performance.ticketsBreakEven.toLong(),
                partialLosses = performance.ticketsPartialLost.toLong(),
                totalLosses = performance.ticketsTotalLost.toLong(),
                winRate = performance.winRate,
                totalStaked = performance.totalStake,
                profitLoss = performance.totalProfit,
                roi = performance.roi,
                avgOdd = performance.avgOdd,
                fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
                partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
                cashoutSuccessRate = calculateCashoutSuccessRate(
                    performance.ticketsPartialWon,
                    performance.ticketsPartialLost
                ),
                firstBetAt = performance.firstBetAt,
                lastSettledAt = performance.lastSettledAt
            )
        }
    }

    // ========== Funções Auxiliares ==========

    /**
     * Calcula taxa percentual: (count / total) * 100
     */
    private fun calculateRate(count: Int, total: Int): BigDecimal {
        if (total == 0) return BigDecimal.ZERO
        return BigDecimal(count)
            .divide(BigDecimal(total), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calcula taxa de sucesso de cashout: (partial_wins / (partial_wins + partial_losses)) * 100
     * Retorna null se não houver cashouts (partial_wins + partial_losses = 0)
     */
    private fun calculateCashoutSuccessRate(partialWins: Int, partialLosses: Int): BigDecimal? {
        val totalPartial = partialWins + partialLosses
        if (totalPartial == 0) return null

        return BigDecimal(partialWins)
            .divide(BigDecimal(totalPartial), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
    }
}
