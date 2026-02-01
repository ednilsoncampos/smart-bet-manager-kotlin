package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.entity.BetSelectionEntity
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
import com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity
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
            successRate = performance.successRate,
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
            partialLossRate = calculateRate(performance.ticketsPartialLost, performance.totalTickets),
            totalLossRate = calculateRate(performance.ticketsTotalLost, performance.totalTickets),
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
        successRate = BigDecimal.ZERO,
        totalStaked = BigDecimal.ZERO,
        totalReturns = BigDecimal.ZERO,
        profitLoss = BigDecimal.ZERO,
        roi = BigDecimal.ZERO,
        avgOdd = null,
        avgStake = null,
        fullWinRate = BigDecimal.ZERO,
        partialWinRate = BigDecimal.ZERO,
        breakEvenRate = BigDecimal.ZERO,
        partialLossRate = BigDecimal.ZERO,
        totalLossRate = BigDecimal.ZERO,
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
                successRate = performance.successRate,
                totalStaked = performance.totalStake,
                profitLoss = performance.totalProfit,
                roi = performance.roi,
                avgOdd = performance.avgOdd,
                fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
                partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
                partialLossRate = calculateRate(performance.ticketsPartialLost, performance.totalTickets),
                totalLossRate = calculateRate(performance.ticketsTotalLost, performance.totalTickets),
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
                successRate = performance.successRate,
                totalStaked = performance.totalStake,
                profitLoss = performance.totalProfit,
                roi = performance.roi,
                avgStake = performance.avgStake,
                fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
                partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
                partialLossRate = calculateRate(performance.ticketsPartialLost, performance.totalTickets),
                totalLossRate = calculateRate(performance.ticketsTotalLost, performance.totalTickets),
                firstBetAt = performance.firstBetAt,
                lastSettledAt = performance.lastSettledAt
            )
        }
    }

    /**
     * Retorna a performance por tipo de mercado usando a tabela analytics.performance_by_market.
     *
     * @param userId ID do usuário
     * @param expandBetBuilder Se true, expande os componentes do Bet Builder e agrega com mercados normais
     */
    fun getPerformanceByMarket(userId: Long, expandBetBuilder: Boolean = false): List<PerformanceByMarketResponse> {
        val performances = byMarketRepository.findByIdUserId(userId)

        return if (expandBetBuilder) {
            expandAndAggregateBetBuilder(userId, performances)
        } else {
            performances.map { performance ->
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
                    successRate = performance.successRate,
                    totalStaked = performance.totalStake,
                    profitLoss = performance.totalProfit,
                    roi = performance.roi,
                    avgOdd = performance.avgOdd,
                    fullWinRate = calculateRate(performance.ticketsFullWon, performance.uniqueTickets),
                    partialWinRate = calculateRate(performance.ticketsPartialWon, performance.uniqueTickets),
                    partialLossRate = calculateRate(performance.ticketsPartialLost, performance.uniqueTickets),
                    totalLossRate = calculateRate(performance.ticketsTotalLost, performance.uniqueTickets),
                    firstBetAt = performance.firstBetAt,
                    lastSettledAt = performance.lastSettledAt,
                    betBuilderComponents = betBuilderStats
                )
            }
        }
    }

    /**
     * Expande componentes do Bet Builder e agrega com mercados normais.
     *
     * Lógica:
     * 1. Remove "Criar Aposta" da lista
     * 2. Extrai componentes individuais do Bet Builder
     * 3. Agrupa componentes por marketName
     * 4. Agrega cada grupo com mercado normal correspondente (se existir)
     * 5. Retorna lista de mercados agregados
     */
    private fun expandAndAggregateBetBuilder(
        userId: Long,
        performances: List<PerformanceByMarketEntity>
    ): List<PerformanceByMarketResponse> {
        // Separa "Criar Aposta" dos outros mercados
        val betBuilderPerformance = performances.find { it.id.marketType == "Criar Aposta" }
        val normalMarkets = performances.filter { it.id.marketType != "Criar Aposta" }

        // Se não houver Bet Builder, retorna mercados normais
        if (betBuilderPerformance == null) {
            return normalMarkets.map { toPerformanceResponse(it, null) }
        }

        // Obtém componentes do Bet Builder e agrupa por marketName
        val components = getBetBuilderComponentStats(userId)
        val componentsByMarket = components.groupBy { it.marketName }

        // Cria mapa de mercados agregados
        val aggregatedMarkets = mutableMapOf<String, AggregatedMarketData>()

        // Adiciona mercados normais ao mapa
        normalMarkets.forEach { market ->
            aggregatedMarkets[market.id.marketType] = AggregatedMarketData(
                marketType = market.id.marketType,
                totalSelections = market.totalSelections,
                uniqueTickets = market.uniqueTickets,
                wins = market.wins,
                losses = market.losses,
                voids = market.voids,
                ticketsFullWon = market.ticketsFullWon,
                ticketsPartialWon = market.ticketsPartialWon,
                ticketsBreakEven = market.ticketsBreakEven,
                ticketsPartialLost = market.ticketsPartialLost,
                ticketsTotalLost = market.ticketsTotalLost,
                totalStake = market.totalStake,
                totalProfit = market.totalProfit,
                firstBetAt = market.firstBetAt,
                lastSettledAt = market.lastSettledAt
            )
        }

        // Agrega componentes do Bet Builder
        for ((marketName, marketComponents) in componentsByMarket) {
            val existing = aggregatedMarkets[marketName]

            if (existing != null) {
                // Mercado já existe, agrega os dados
                existing.totalSelections += marketComponents.size
                existing.uniqueTickets += betBuilderPerformance.uniqueTickets
                existing.wins += marketComponents.sumOf { it.wins.toInt() }
                existing.losses += marketComponents.sumOf { it.losses.toInt() }
                // voids permanecem 0 para componentes do Bet Builder

                // Agrega dados do ticket (proporcionalmente - assumindo distribuição igual)
                existing.ticketsFullWon += betBuilderPerformance.ticketsFullWon
                existing.ticketsPartialWon += betBuilderPerformance.ticketsPartialWon
                existing.ticketsBreakEven += betBuilderPerformance.ticketsBreakEven
                existing.ticketsPartialLost += betBuilderPerformance.ticketsPartialLost
                existing.ticketsTotalLost += betBuilderPerformance.ticketsTotalLost
                existing.totalStake += betBuilderPerformance.totalStake
                existing.totalProfit += betBuilderPerformance.totalProfit

                // Atualiza timestamps
                existing.firstBetAt = minOf(existing.firstBetAt ?: Long.MAX_VALUE, betBuilderPerformance.firstBetAt ?: Long.MAX_VALUE)
                existing.lastSettledAt = maxOf(existing.lastSettledAt, betBuilderPerformance.lastSettledAt)
            } else {
                // Novo mercado do Bet Builder
                aggregatedMarkets[marketName] = AggregatedMarketData(
                    marketType = marketName,
                    totalSelections = marketComponents.size,
                    uniqueTickets = betBuilderPerformance.uniqueTickets,
                    wins = marketComponents.sumOf { it.wins.toInt() },
                    losses = marketComponents.sumOf { it.losses.toInt() },
                    voids = 0,
                    ticketsFullWon = betBuilderPerformance.ticketsFullWon,
                    ticketsPartialWon = betBuilderPerformance.ticketsPartialWon,
                    ticketsBreakEven = betBuilderPerformance.ticketsBreakEven,
                    ticketsPartialLost = betBuilderPerformance.ticketsPartialLost,
                    ticketsTotalLost = betBuilderPerformance.ticketsTotalLost,
                    totalStake = betBuilderPerformance.totalStake,
                    totalProfit = betBuilderPerformance.totalProfit,
                    firstBetAt = betBuilderPerformance.firstBetAt,
                    lastSettledAt = betBuilderPerformance.lastSettledAt
                )
            }
        }

        // Converte para PerformanceByMarketResponse
        return aggregatedMarkets.values.map { data ->
            PerformanceByMarketResponse(
                marketType = data.marketType,
                totalSelections = data.totalSelections.toLong(),
                uniqueTickets = data.uniqueTickets.toLong(),
                wins = data.wins.toLong(),
                losses = data.losses.toLong(),
                voids = data.voids.toLong(),
                fullWins = data.ticketsFullWon.toLong(),
                partialWins = data.ticketsPartialWon.toLong(),
                breakEven = data.ticketsBreakEven.toLong(),
                partialLosses = data.ticketsPartialLost.toLong(),
                totalLosses = data.ticketsTotalLost.toLong(),
                // Taxas corrigidas: winRate baseado em SELEÇÕES, successRate baseado em TICKETS
                winRate = calculateRate(data.wins, data.totalSelections),
                successRate = calculateRate(data.ticketsFullWon + data.ticketsPartialWon, data.uniqueTickets),
                totalStaked = data.totalStake,
                profitLoss = data.totalProfit,
                roi = calculateRoi(data.totalProfit, data.totalStake),
                avgOdd = null, // Média de odds é complexa de agregar
                fullWinRate = calculateRate(data.ticketsFullWon, data.uniqueTickets),
                partialWinRate = calculateRate(data.ticketsPartialWon, data.uniqueTickets),
                partialLossRate = calculateRate(data.ticketsPartialLost, data.uniqueTickets),
                totalLossRate = calculateRate(data.ticketsTotalLost, data.uniqueTickets),
                firstBetAt = data.firstBetAt,
                lastSettledAt = data.lastSettledAt,
                betBuilderComponents = null // Não faz sentido em mercados expandidos
            )
        }.sortedByDescending { it.totalSelections }
    }

    /**
     * Converte PerformanceByMarketEntity para PerformanceByMarketResponse
     */
    private fun toPerformanceResponse(
        performance: PerformanceByMarketEntity,
        betBuilderStats: List<BetBuilderComponentStats>?
    ): PerformanceByMarketResponse {
        return PerformanceByMarketResponse(
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
            successRate = performance.successRate,
            totalStaked = performance.totalStake,
            profitLoss = performance.totalProfit,
            roi = performance.roi,
            avgOdd = performance.avgOdd,
            fullWinRate = calculateRate(performance.ticketsFullWon, performance.uniqueTickets),
            partialWinRate = calculateRate(performance.ticketsPartialWon, performance.uniqueTickets),
            partialLossRate = calculateRate(performance.ticketsPartialLost, performance.uniqueTickets),
            totalLossRate = calculateRate(performance.ticketsTotalLost, performance.uniqueTickets),
            firstBetAt = performance.firstBetAt,
            lastSettledAt = performance.lastSettledAt,
            betBuilderComponents = betBuilderStats
        )
    }

    /**
     * Estrutura auxiliar para agregação de dados de mercado
     */
    private data class AggregatedMarketData(
        val marketType: String,
        var totalSelections: Int,
        var uniqueTickets: Int,
        var wins: Int,
        var losses: Int,
        var voids: Int,
        var ticketsFullWon: Int,
        var ticketsPartialWon: Int,
        var ticketsBreakEven: Int,
        var ticketsPartialLost: Int,
        var ticketsTotalLost: Int,
        var totalStake: BigDecimal,
        var totalProfit: BigDecimal,
        var firstBetAt: Long?,
        var lastSettledAt: Long
    )

    /**
     * Calcula ROI: (profit / stake) * 100
     */
    private fun calculateRoi(profit: BigDecimal, stake: BigDecimal): BigDecimal {
        if (stake == BigDecimal.ZERO) return BigDecimal.ZERO
        return profit.divide(stake, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(4, RoundingMode.HALF_UP)
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
                successRate = performance.successRate,
                totalStaked = performance.totalStake,
                profitLoss = performance.totalProfit,
                roi = performance.roi,
                avgOdd = performance.avgOdd,
                fullWinRate = calculateRate(performance.ticketsFullWon, performance.totalTickets),
                partialWinRate = calculateRate(performance.ticketsPartialWon, performance.totalTickets),
                partialLossRate = calculateRate(performance.ticketsPartialLost, performance.totalTickets),
                totalLossRate = calculateRate(performance.ticketsTotalLost, performance.totalTickets),
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
