package com.smartbet.application.usecase

import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.event.TicketSettledEvent
import com.smartbet.infrastructure.persistence.entity.*
import com.smartbet.infrastructure.persistence.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

/**
 * Serviço responsável por agregar dados de performance em analytics.
 *
 * Processa eventos de liquidação de tickets e atualiza todas as tabelas
 * de analytics de forma incremental usando UPSERT lógico no backend.
 *
 * Este serviço é chamado de forma assíncrona através de eventos Spring,
 * rodando em uma transação separada da liquidação do ticket.
 */
@Service
class AnalyticsAggregationService(
    private val overallRepository: PerformanceOverallRepository,
    private val byMonthRepository: PerformanceByMonthRepository,
    private val byProviderRepository: PerformanceByProviderRepository,
    private val byMarketRepository: PerformanceByMarketRepository,
    private val byTournamentRepository: PerformanceByTournamentRepository
) {
    private val logger = LoggerFactory.getLogger(AnalyticsAggregationService::class.java)

    /**
     * Atualiza todas as tabelas de analytics baseado em um ticket liquidado.
     *
     * @param event Evento de liquidação do ticket
     *
     * TODO: Reescrever para usar os campos corretos das entidades (ticketsWon, ticketsLost, uniqueTickets, totalSelections, etc.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Deprecated("Precisa ser reescrito para bater com o schema do banco")
    fun updateOnSettlement(event: TicketSettledEvent) {
        logger.info(
            "Processing analytics update for ticket {} (user: {}, status: {}, profit: {})",
            event.ticketId, event.userId, event.financialStatus, event.profitLoss
        )

        try {
            // Atualiza todas as tabelas
            updateOverall(event)
            updateByMonth(event)
            updateByProvider(event)
            updateByMarket(event)
            updateByTournament(event)

            logger.info("Analytics successfully updated for ticket {}", event.ticketId)
        } catch (e: Exception) {
            logger.error("Error updating analytics for ticket {}: {}", event.ticketId, e.message, e)
            throw e // Permite retry do @Retryable no listener
        }
    }

    /**
     * Atualiza a tabela analytics.performance_overall (all-time)
     */
    private fun updateOverall(event: TicketSettledEvent) {
        val existing = overallRepository.findByUserId(event.userId)

        if (existing == null) {
            // Primeiro ticket do usuário - cria novo registro
            val newEntity = createNewOverall(event)
            overallRepository.save(newEntity)
            logger.debug("Created new overall performance for user {}", event.userId)
        } else {
            // Atualiza registro existente
            updateExistingOverall(existing, event)
            overallRepository.save(existing)
            logger.debug("Updated overall performance for user {}", event.userId)
        }
    }

    /**
     * Atualiza a tabela analytics.performance_by_month
     */
    private fun updateByMonth(event: TicketSettledEvent) {
        val (year, month) = extractYearMonth(event.settledAt)
        val id = PerformanceByMonthId(event.userId, year, month)

        val existing = byMonthRepository.findByIdUserIdAndIdYearAndIdMonth(event.userId, year, month)

        if (existing == null) {
            // Primeiro ticket do mês - cria novo registro
            val newEntity = createNewByMonth(id, event)
            byMonthRepository.save(newEntity)
            logger.debug("Created new monthly performance for user {} ({}/{})", event.userId, year, month)
        } else {
            // Atualiza registro existente
            updateExistingByMonth(existing, event)
            byMonthRepository.save(existing)
            logger.debug("Updated monthly performance for user {} ({}/{})", event.userId, year, month)
        }
    }

    /**
     * Atualiza a tabela analytics.performance_by_provider
     */
    private fun updateByProvider(event: TicketSettledEvent) {
        val id = PerformanceByProviderId(event.userId, event.providerId)
        val existing = byProviderRepository.findByIdUserIdAndIdProviderId(event.userId, event.providerId)

        if (existing == null) {
            // Primeiro ticket com este provider - cria novo registro
            val newEntity = createNewByProvider(id, event)
            byProviderRepository.save(newEntity)
            logger.debug("Created new provider performance for user {} (provider: {})", event.userId, event.providerId)
        } else {
            // Atualiza registro existente
            updateExistingByProvider(existing, event)
            byProviderRepository.save(existing)
            logger.debug("Updated provider performance for user {} (provider: {})", event.userId, event.providerId)
        }
    }

    /**
     * Atualiza a tabela analytics.performance_by_market
     *
     * Como um ticket pode ter múltiplas seleções com diferentes mercados,
     * atualiza todos os mercados envolvidos.
     */
    private fun updateByMarket(event: TicketSettledEvent) {
        // Agrupa seleções por tipo de mercado
        val marketTypes = event.selections.map { it.marketType }.distinct()

        for (marketType in marketTypes) {
            val id = PerformanceByMarketId(event.userId, marketType)
            val existing = byMarketRepository.findByIdUserIdAndIdMarketType(event.userId, marketType)

            if (existing == null) {
                // Primeiro ticket com este mercado - cria novo registro
                val newEntity = createNewByMarket(id, event)
                byMarketRepository.save(newEntity)
                logger.debug("Created new market performance for user {} (market: {})", event.userId, marketType)
            } else {
                // Atualiza registro existente
                updateExistingByMarket(existing, event)
                byMarketRepository.save(existing)
                logger.debug("Updated market performance for user {} (market: {})", event.userId, marketType)
            }
        }
    }

    /**
     * Atualiza a tabela analytics.performance_by_tournament
     *
     * Como um ticket pode ter múltiplas seleções em diferentes torneios,
     * atualiza todos os torneios envolvidos.
     */
    private fun updateByTournament(event: TicketSettledEvent) {
        // Agrupa seleções por torneio
        val tournamentIds = event.selections.mapNotNull { it.tournamentId }.distinct()

        for (tournamentId in tournamentIds) {
            val id = PerformanceByTournamentId(event.userId, tournamentId)
            val existing = byTournamentRepository.findByIdUserIdAndIdTournamentId(event.userId, tournamentId)

            if (existing == null) {
                // Primeiro ticket com este torneio - cria novo registro
                val newEntity = createNewByTournament(id, event)
                byTournamentRepository.save(newEntity)
                logger.debug("Created new tournament performance for user {} (tournament: {})", event.userId, tournamentId)
            } else {
                // Atualiza registro existente
                updateExistingByTournament(existing, event)
                byTournamentRepository.save(existing)
                logger.debug("Updated tournament performance for user {} (tournament: {})", event.userId, tournamentId)
            }
        }
    }

    // ========== Criação de Novos Registros ==========

    private fun createNewOverall(event: TicketSettledEvent): PerformanceOverallEntity {
        val (wins, losses, voids) = countTicketsByStatus(event)
        val totalReturn = event.actualPayout

        return PerformanceOverallEntity(
            userId = event.userId,
            totalTickets = 1,
            ticketsWon = wins,
            ticketsLost = losses,
            ticketsVoid = voids,
            ticketsCashedOut = 0,
            totalStake = event.stake,
            totalReturn = totalReturn,
            totalProfit = event.profitLoss,
            roi = event.roi,
            winRate = calculateWinRate(wins, 1),
            avgOdd = null,
            avgStake = event.stake,
            currentStreak = if (wins > 0) 1 else if (losses > 0) -1 else 0,
            bestWinStreak = if (wins > 0) 1 else 0,
            worstLossStreak = if (losses > 0) -1 else 0,
            biggestWin = if (event.profitLoss > BigDecimal.ZERO) event.profitLoss else null,
            biggestLoss = if (event.profitLoss < BigDecimal.ZERO) event.profitLoss else null,
            bestRoiTicket = event.roi,
            firstBetAt = event.settledAt,
            lastSettledAt = event.settledAt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createNewByMonth(id: PerformanceByMonthId, event: TicketSettledEvent): PerformanceByMonthEntity {
        val (wins, losses, voids) = countTicketsByStatus(event)

        return PerformanceByMonthEntity(
            id = id,
            totalTickets = 1,
            ticketsWon = wins,
            ticketsLost = losses,
            ticketsVoid = voids,
            totalStake = event.stake,
            totalProfit = event.profitLoss,
            roi = event.roi,
            winRate = calculateWinRate(wins, 1),
            avgStake = event.stake,
            firstBetAt = event.settledAt,
            lastSettledAt = event.settledAt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createNewByProvider(id: PerformanceByProviderId, event: TicketSettledEvent): PerformanceByProviderEntity {
        val (wins, losses, voids) = countTicketsByStatus(event)

        return PerformanceByProviderEntity(
            id = id,
            totalTickets = 1,
            ticketsWon = wins,
            ticketsLost = losses,
            ticketsVoid = voids,
            ticketsCashedOut = 0,
            totalStake = event.stake,
            totalProfit = event.profitLoss,
            roi = event.roi,
            winRate = calculateWinRate(wins, 1),
            avgOdd = null,
            firstBetAt = event.settledAt,
            lastSettledAt = event.settledAt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createNewByMarket(id: PerformanceByMarketId, event: TicketSettledEvent): PerformanceByMarketEntity {
        val (wins, losses, voids) = countTicketsByStatus(event)
        val selectionsInMarket = event.selections.count { it.marketType == id.marketType }

        return PerformanceByMarketEntity(
            id = id,
            totalSelections = selectionsInMarket,
            wins = wins,
            losses = losses,
            voids = voids,
            uniqueTickets = 1,
            totalStake = event.stake,
            totalProfit = event.profitLoss,
            roi = event.roi,
            winRate = calculateWinRate(wins, 1),
            avgOdd = null,
            firstBetAt = event.settledAt,
            lastSettledAt = event.settledAt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createNewByTournament(id: PerformanceByTournamentId, event: TicketSettledEvent): PerformanceByTournamentEntity {
        val (wins, losses, voids) = countTicketsByStatus(event)

        return PerformanceByTournamentEntity(
            id = id,
            totalTickets = 1,
            ticketsWon = wins,
            ticketsLost = losses,
            ticketsVoid = voids,
            totalStake = event.stake,
            totalProfit = event.profitLoss,
            roi = event.roi,
            winRate = calculateWinRate(wins, 1),
            avgOdd = null,
            firstBetAt = event.settledAt,
            lastSettledAt = event.settledAt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // ========== Atualização de Registros Existentes ==========

    private fun updateExistingOverall(entity: PerformanceOverallEntity, event: TicketSettledEvent) {
        val (wins, losses, voids) = countTicketsByStatus(event)

        // Atualiza contadores
        entity.totalTickets++
        entity.ticketsWon += wins
        entity.ticketsLost += losses
        entity.ticketsVoid += voids

        // Atualiza valores financeiros
        entity.totalStake += event.stake
        entity.totalReturn += event.actualPayout
        entity.totalProfit += event.profitLoss

        // Recalcula métricas
        entity.roi = calculateRoi(entity.totalProfit, entity.totalStake)
        entity.winRate = calculateWinRate(entity.ticketsWon, entity.totalTickets)

        // Atualiza gamificação: Streaks
        updateStreaks(entity, event.financialStatus)

        // Atualiza gamificação: Records
        updateRecords(entity, event.profitLoss)

        // Atualiza timestamps
        entity.lastSettledAt = event.settledAt
        entity.updatedAt = System.currentTimeMillis()
    }

    private fun updateExistingByMonth(entity: PerformanceByMonthEntity, event: TicketSettledEvent) {
        val (wins, losses, voids) = countTicketsByStatus(event)

        entity.totalTickets++
        entity.ticketsWon += wins
        entity.ticketsLost += losses
        entity.ticketsVoid += voids
        entity.totalStake += event.stake
        entity.totalProfit += event.profitLoss
        entity.roi = calculateRoi(entity.totalProfit, entity.totalStake)
        entity.winRate = calculateWinRate(entity.ticketsWon, entity.totalTickets)
        entity.lastSettledAt = event.settledAt
        entity.updatedAt = System.currentTimeMillis()
    }

    private fun updateExistingByProvider(entity: PerformanceByProviderEntity, event: TicketSettledEvent) {
        val (wins, losses, voids) = countTicketsByStatus(event)

        entity.totalTickets++
        entity.ticketsWon += wins
        entity.ticketsLost += losses
        entity.ticketsVoid += voids
        entity.totalStake += event.stake
        entity.totalProfit += event.profitLoss
        entity.roi = calculateRoi(entity.totalProfit, entity.totalStake)
        entity.winRate = calculateWinRate(entity.ticketsWon, entity.totalTickets)
        entity.lastSettledAt = event.settledAt
        entity.updatedAt = System.currentTimeMillis()
    }

    private fun updateExistingByMarket(entity: PerformanceByMarketEntity, event: TicketSettledEvent) {
        // TODO: Reescrever lógica para usar totalSelections e uniqueTickets
        val (wins, losses, voids) = countTicketsByStatus(event)
        val selectionsInMarket = event.selections.count { it.marketType == entity.id.marketType }

        entity.totalSelections += selectionsInMarket
        entity.uniqueTickets++
        entity.wins += wins
        entity.losses += losses
        entity.voids += voids
        entity.totalStake += event.stake
        entity.totalProfit += event.profitLoss
        entity.roi = calculateRoi(entity.totalProfit, entity.totalStake)
        entity.winRate = calculateWinRate(entity.wins, entity.uniqueTickets)
        entity.lastSettledAt = event.settledAt
        entity.updatedAt = System.currentTimeMillis()
    }

    private fun updateExistingByTournament(entity: PerformanceByTournamentEntity, event: TicketSettledEvent) {
        val (wins, losses, voids) = countTicketsByStatus(event)

        entity.totalTickets++
        entity.ticketsWon += wins
        entity.ticketsLost += losses
        entity.ticketsVoid += voids
        entity.totalStake += event.stake
        entity.totalProfit += event.profitLoss
        entity.roi = calculateRoi(entity.totalProfit, entity.totalStake)
        entity.winRate = calculateWinRate(entity.ticketsWon, entity.totalTickets)
        entity.lastSettledAt = event.settledAt
        entity.updatedAt = System.currentTimeMillis()
    }

    // ========== Lógica de Gamificação ==========

    /**
     * Atualiza as sequências (streaks) de vitórias e derrotas.
     * currentStreak é positivo para vitórias, negativo para derrotas.
     */
    private fun updateStreaks(entity: PerformanceOverallEntity, status: FinancialStatus) {
        when (status) {
            FinancialStatus.FULL_WIN, FinancialStatus.PARTIAL_WIN -> {
                // Incrementa streak de vitórias (positivo)
                entity.currentStreak = if (entity.currentStreak >= 0) {
                    entity.currentStreak + 1
                } else {
                    1 // Reseta se estava em loss streak
                }

                // Atualiza record de melhor sequência de vitórias
                if (entity.currentStreak > entity.bestWinStreak) {
                    entity.bestWinStreak = entity.currentStreak
                }
            }

            FinancialStatus.TOTAL_LOSS, FinancialStatus.PARTIAL_LOSS -> {
                // Incrementa streak de derrotas (negativo)
                entity.currentStreak = if (entity.currentStreak <= 0) {
                    entity.currentStreak - 1
                } else {
                    -1 // Reseta se estava em win streak
                }

                // Atualiza record de pior sequência de derrotas
                if (entity.currentStreak < entity.worstLossStreak) {
                    entity.worstLossStreak = entity.currentStreak
                }
            }

            FinancialStatus.BREAK_EVEN, FinancialStatus.PENDING -> {
                // Break even ou pending reseta a streak
                entity.currentStreak = 0
            }
        }
    }

    /**
     * Atualiza os records de maior vitória e maior perda
     */
    private fun updateRecords(entity: PerformanceOverallEntity, profitLoss: BigDecimal) {
        if (profitLoss > BigDecimal.ZERO) {
            // Vitória
            if (entity.biggestWin == null || profitLoss > entity.biggestWin) {
                entity.biggestWin = profitLoss
            }
        } else if (profitLoss < BigDecimal.ZERO) {
            // Perda (valores negativos)
            if (entity.biggestLoss == null || profitLoss < entity.biggestLoss) {
                entity.biggestLoss = profitLoss
            }
        }
    }

    // ========== Funções Auxiliares ==========

    /**
     * Conta tickets por status financeiro.
     *
     * @return Triple(wins, losses, voids)
     */
    private fun countTicketsByStatus(event: TicketSettledEvent): Triple<Int, Int, Int> {
        return when (event.financialStatus) {
            FinancialStatus.FULL_WIN, FinancialStatus.PARTIAL_WIN -> Triple(1, 0, 0)
            FinancialStatus.TOTAL_LOSS, FinancialStatus.PARTIAL_LOSS -> Triple(0, 1, 0)
            FinancialStatus.BREAK_EVEN -> Triple(0, 0, 1)
            FinancialStatus.PENDING -> Triple(0, 0, 0) // Não deveria acontecer
        }
    }

    /**
     * Calcula ROI: (profit / stake) * 100
     */
    private fun calculateRoi(profit: BigDecimal, stake: BigDecimal): BigDecimal {
        if (stake == BigDecimal.ZERO) return BigDecimal.ZERO
        return (profit.divide(stake, 6, RoundingMode.HALF_UP) * BigDecimal(100))
            .setScale(4, RoundingMode.HALF_UP)
    }

    /**
     * Calcula win rate: (wins / total) * 100
     */
    private fun calculateWinRate(wins: Int, total: Int): BigDecimal {
        if (total == 0) return BigDecimal.ZERO
        return BigDecimal(wins)
            .divide(BigDecimal(total), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Extrai ano e mês de um timestamp epoch em millis.
     *
     * @return Pair(year, month)
     */
    private fun extractYearMonth(epochMillis: Long): Pair<Int, Int> {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        return Pair(zonedDateTime.year, zonedDateTime.monthValue)
    }
}
