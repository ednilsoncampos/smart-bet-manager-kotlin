package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.entity.BetSelectionEntity
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
import com.smartbet.infrastructure.persistence.repository.BetSelectionRepository
import com.smartbet.infrastructure.persistence.repository.BetTicketRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class PerformanceAnalyticService(
    private val ticketRepository: BetTicketRepository,
    private val selectionRepository: BetSelectionRepository,
    private val providerRepository: BettingProviderRepository
) {
    
    /**
     * Retorna a performance geral do usuário.
     */
    fun getOverallPerformance(userId: Long): OverallPerformanceResponse {
        val pageable = PageRequest.of(0, Int.MAX_VALUE)
        val allTickets = ticketRepository.findByUserId(userId, pageable).content
        
        val totalBets = allTickets.size.toLong()
        val settledBets = allTickets.count { it.ticketStatus != TicketStatus.OPEN }.toLong()
        val openBets = allTickets.count { it.ticketStatus == TicketStatus.OPEN }.toLong()
        
        // Contagem detalhada por status financeiro
        val fullWins = allTickets.count { it.financialStatus == FinancialStatus.FULL_WIN }.toLong()
        val partialWins = allTickets.count { it.financialStatus == FinancialStatus.PARTIAL_WIN }.toLong()
        val breakEven = allTickets.count { it.financialStatus == FinancialStatus.BREAK_EVEN }.toLong()
        val partialLosses = allTickets.count { it.financialStatus == FinancialStatus.PARTIAL_LOSS }.toLong()
        val totalLosses = allTickets.count { it.financialStatus == FinancialStatus.TOTAL_LOSS }.toLong()
        
        // Totais agregados
        val wins = fullWins + partialWins
        val losses = totalLosses + partialLosses
        
        val winRate = if (settledBets > 0) {
            BigDecimal.valueOf(wins)
                .divide(BigDecimal.valueOf(settledBets), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
        } else {
            BigDecimal.ZERO
        }
        
        val settledTickets = allTickets.filter { it.ticketStatus != TicketStatus.OPEN }
        val totalStaked = settledTickets.fold(BigDecimal.ZERO) { acc, ticket -> acc + ticket.stake }
        val totalReturns = settledTickets.fold(BigDecimal.ZERO) { acc, ticket -> acc + (ticket.actualPayout ?: BigDecimal.ZERO) }
        val profitLoss = totalReturns - totalStaked
        
        val roi = if (totalStaked > BigDecimal.ZERO) {
            profitLoss
                .divide(totalStaked, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
        } else {
            BigDecimal.ZERO
        }
        
        // Calcula a mediana das odds (mais resistente a outliers)
        val medianOdd = if (allTickets.isNotEmpty()) {
            val sortedOdds = allTickets.map { it.totalOdd }.sorted()
            val size = sortedOdds.size
            if (size % 2 == 0) {
                // Média dos dois valores centrais
                (sortedOdds[size / 2 - 1] + sortedOdds[size / 2])
                    .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
            } else {
                // Valor central
                sortedOdds[size / 2]
            }
        } else {
            BigDecimal.ZERO
        }
        
        val averageStake = if (allTickets.isNotEmpty()) {
            allTickets.fold(BigDecimal.ZERO) { acc, ticket -> acc + ticket.stake }
                .divide(BigDecimal.valueOf(allTickets.size.toLong()), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        return OverallPerformanceResponse(
            totalBets = totalBets,
            settledBets = settledBets,
            openBets = openBets,
            fullWins = fullWins,
            partialWins = partialWins,
            breakEven = breakEven,
            partialLosses = partialLosses,
            totalLosses = totalLosses,
            wins = wins,
            losses = losses,
            winRate = winRate,
            totalStaked = totalStaked,
            totalReturns = totalReturns,
            profitLoss = profitLoss,
            roi = roi,
            medianOdd = medianOdd,
            averageStake = averageStake
        )
    }
    
    /**
     * Retorna a performance por campeonato/torneio.
     */
    fun getPerformanceByTournament(userId: Long): List<PerformanceByTournamentResponse> {
        val pageable = PageRequest.of(0, Int.MAX_VALUE)
        val allTickets = ticketRepository.findByUserId(userId, pageable).content
            .filter { it.ticketStatus != TicketStatus.OPEN }
        
        // Agrupa por torneio através das seleções
        data class TournamentData(val tournamentName: String, val selection: BetSelectionEntity, val financialStatus: FinancialStatus)
        
        val selectionsByTournament: Map<String, List<TournamentData>> = allTickets
            .flatMap { ticket: BetTicketEntity -> 
                ticket.selections.mapNotNull { selection: BetSelectionEntity ->
                    selection.tournament?.name?.let { name: String ->
                        TournamentData(name, selection, ticket.financialStatus)
                    }
                }
            }
            .groupBy { it.tournamentName }
        
        return selectionsByTournament.map { (tournamentName: String, selections: List<TournamentData>) ->
            val totalBets = selections.size.toLong()
            
            // Contagem detalhada por status financeiro do ticket
            val fullWins = selections.count { it.financialStatus == FinancialStatus.FULL_WIN }.toLong()
            val partialWins = selections.count { it.financialStatus == FinancialStatus.PARTIAL_WIN }.toLong()
            val breakEven = selections.count { it.financialStatus == FinancialStatus.BREAK_EVEN }.toLong()
            val partialLosses = selections.count { it.financialStatus == FinancialStatus.PARTIAL_LOSS }.toLong()
            val totalLosses = selections.count { it.financialStatus == FinancialStatus.TOTAL_LOSS }.toLong()
            
            // Totais agregados
            val wins = fullWins + partialWins
            val losses = totalLosses + partialLosses
            
            val winRate = if (totalBets > 0) {
                BigDecimal.valueOf(wins)
                    .divide(BigDecimal.valueOf(totalBets), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
            } else {
                BigDecimal.ZERO
            }
            
            PerformanceByTournamentResponse(
                tournamentName = tournamentName,
                totalBets = totalBets,
                fullWins = fullWins,
                partialWins = partialWins,
                breakEven = breakEven,
                partialLosses = partialLosses,
                totalLosses = totalLosses,
                wins = wins,
                losses = losses,
                winRate = winRate
            )
        }
    }
    
    /**
     * Retorna a performance por tipo de mercado.
     */
    fun getPerformanceByMarket(userId: Long): List<PerformanceByMarketResponse> {
        val pageable = PageRequest.of(0, Int.MAX_VALUE)
        val allTickets = ticketRepository.findByUserId(userId, pageable).content
            .filter { it.ticketStatus != TicketStatus.OPEN }
        
        // Agrupa por mercado através das seleções
        data class MarketData(val marketType: String, val selection: BetSelectionEntity, val financialStatus: FinancialStatus)
        
        val selectionsByMarket: Map<String, List<MarketData>> = allTickets
            .flatMap { ticket: BetTicketEntity -> 
                ticket.selections.mapNotNull { selection: BetSelectionEntity ->
                    selection.marketType?.let { market: String ->
                        MarketData(market, selection, ticket.financialStatus)
                    }
                }
            }
            .groupBy { it.marketType }
        
        return selectionsByMarket.map { (marketType: String, selections: List<MarketData>) ->
            val totalBets = selections.size.toLong()
            
            // Contagem detalhada por status financeiro do ticket
            val fullWins = selections.count { it.financialStatus == FinancialStatus.FULL_WIN }.toLong()
            val partialWins = selections.count { it.financialStatus == FinancialStatus.PARTIAL_WIN }.toLong()
            val breakEven = selections.count { it.financialStatus == FinancialStatus.BREAK_EVEN }.toLong()
            val partialLosses = selections.count { it.financialStatus == FinancialStatus.PARTIAL_LOSS }.toLong()
            val totalLosses = selections.count { it.financialStatus == FinancialStatus.TOTAL_LOSS }.toLong()
            
            // Totais agregados
            val wins = fullWins + partialWins
            val losses = totalLosses + partialLosses
            
            val winRate = if (totalBets > 0) {
                BigDecimal.valueOf(wins)
                    .divide(BigDecimal.valueOf(totalBets), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
            } else {
                BigDecimal.ZERO
            }
            
            PerformanceByMarketResponse(
                marketType = marketType,
                totalBets = totalBets,
                fullWins = fullWins,
                partialWins = partialWins,
                breakEven = breakEven,
                partialLosses = partialLosses,
                totalLosses = totalLosses,
                wins = wins,
                losses = losses,
                winRate = winRate
            )
        }
    }
    
    /**
     * Retorna a performance por casa de apostas.
     * Inclui contagem detalhada de todos os status financeiros.
     */
    fun getPerformanceByProvider(userId: Long): List<PerformanceByProviderResponse> {
        val pageable = PageRequest.of(0, Int.MAX_VALUE)
        val allTickets = ticketRepository.findByUserId(userId, pageable).content
        val providers = providerRepository.findAll().associateBy { it.id }
        
        return allTickets
            .filter { it.ticketStatus != TicketStatus.OPEN }
            .groupBy { it.providerId }
            .map { (providerId: Long, tickets: List<BetTicketEntity>) ->
                val provider = providers[providerId]
                val totalBets = tickets.size.toLong()
                
                // Contagem detalhada por status financeiro
                val fullWins = tickets.count { it.financialStatus == FinancialStatus.FULL_WIN }.toLong()
                val partialWins = tickets.count { it.financialStatus == FinancialStatus.PARTIAL_WIN }.toLong()
                val breakEven = tickets.count { it.financialStatus == FinancialStatus.BREAK_EVEN }.toLong()
                val partialLosses = tickets.count { it.financialStatus == FinancialStatus.PARTIAL_LOSS }.toLong()
                val totalLosses = tickets.count { it.financialStatus == FinancialStatus.TOTAL_LOSS }.toLong()
                
                // Totais agregados
                val wins = fullWins + partialWins
                val losses = totalLosses + partialLosses
                
                val winRate = if (totalBets > 0) {
                    BigDecimal.valueOf(wins)
                        .divide(BigDecimal.valueOf(totalBets), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                } else {
                    BigDecimal.ZERO
                }
                
                val totalStaked = tickets.fold(BigDecimal.ZERO) { acc, ticket -> acc + ticket.stake }
                val profitLoss = tickets.fold(BigDecimal.ZERO) { acc, ticket -> acc + ticket.profitLoss }
                
                val roi = if (totalStaked > BigDecimal.ZERO) {
                    profitLoss
                        .divide(totalStaked, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                } else {
                    BigDecimal.ZERO
                }
                
                PerformanceByProviderResponse(
                    providerId = providerId,
                    providerName = provider?.name ?: "Desconhecido",
                    totalBets = totalBets,
                    fullWins = fullWins,
                    partialWins = partialWins,
                    breakEven = breakEven,
                    partialLosses = partialLosses,
                    totalLosses = totalLosses,
                    wins = wins,
                    losses = losses,
                    winRate = winRate,
                    profitLoss = profitLoss,
                    roi = roi
                )
            }
    }
}
