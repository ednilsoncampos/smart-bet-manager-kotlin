package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.repository.BetSelectionRepository
import com.smartbet.infrastructure.persistence.repository.BetTicketRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class AnalyticsService(
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
        
        val wins = allTickets.count { 
            it.financialStatus in listOf(FinancialStatus.FULL_WIN, FinancialStatus.PARTIAL_WIN) 
        }.toLong()
        
        val losses = allTickets.count { 
            it.financialStatus in listOf(FinancialStatus.TOTAL_LOSS, FinancialStatus.PARTIAL_LOSS) 
        }.toLong()
        
        val winRate = if (settledBets > 0) {
            BigDecimal(wins)
                .divide(BigDecimal(settledBets), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        val settledTickets = allTickets.filter { it.ticketStatus != TicketStatus.OPEN }
        val totalStaked = settledTickets.sumOf { it.stake }
        val totalReturns = settledTickets.sumOf { it.actualPayout ?: BigDecimal.ZERO }
        val profitLoss = totalReturns - totalStaked
        
        val roi = if (totalStaked > BigDecimal.ZERO) {
            profitLoss
                .divide(totalStaked, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
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
                    .divide(BigDecimal(2), 4, RoundingMode.HALF_UP)
            } else {
                // Valor central
                sortedOdds[size / 2]
            }
        } else {
            BigDecimal.ZERO
        }
        
        val averageStake = if (allTickets.isNotEmpty()) {
            allTickets.sumOf { it.stake }
                .divide(BigDecimal(allTickets.size), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        return OverallPerformanceResponse(
            totalBets = totalBets,
            settledBets = settledBets,
            openBets = openBets,
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
        val stats = selectionRepository.getStatsByTournament(userId)
        
        return stats.map { row ->
            val tournamentName = row[0] as String
            val totalBets = (row[1] as Number).toLong()
            val wins = (row[2] as Number).toLong()
            val losses = totalBets - wins
            
            val winRate = if (totalBets > 0) {
                BigDecimal(wins)
                    .divide(BigDecimal(totalBets), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
            
            PerformanceByTournamentResponse(
                tournamentName = tournamentName,
                totalBets = totalBets,
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
        val stats = selectionRepository.getStatsByMarket(userId)
        
        return stats.map { row ->
            val marketType = row[0] as String
            val totalBets = (row[1] as Number).toLong()
            val wins = (row[2] as Number).toLong()
            val losses = totalBets - wins
            
            val winRate = if (totalBets > 0) {
                BigDecimal(wins)
                    .divide(BigDecimal(totalBets), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
            
            PerformanceByMarketResponse(
                marketType = marketType,
                totalBets = totalBets,
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
            .map { (providerId, tickets) ->
                val provider = providers[providerId]
                val totalBets = tickets.size.toLong()
                
                // Contagem detalhada por status financeiro
                val fullWins = tickets.count { it.financialStatus == FinancialStatus.FULL_WIN }.toLong()
                val partialWins = tickets.count { it.financialStatus == FinancialStatus.PARTIAL_WIN }.toLong()
                val totalLosses = tickets.count { it.financialStatus == FinancialStatus.TOTAL_LOSS }.toLong()
                val partialLosses = tickets.count { it.financialStatus == FinancialStatus.PARTIAL_LOSS }.toLong()
                val breakEven = tickets.count { it.financialStatus == FinancialStatus.BREAK_EVEN }.toLong()
                
                // Totais agregados
                val wins = fullWins + partialWins
                val losses = totalLosses + partialLosses
                
                val winRate = if (totalBets > 0) {
                    BigDecimal(wins)
                        .divide(BigDecimal(totalBets), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal(100))
                } else {
                    BigDecimal.ZERO
                }
                
                val totalStaked = tickets.sumOf { it.stake }
                val profitLoss = tickets.sumOf { it.profitLoss }
                
                val roi = if (totalStaked > BigDecimal.ZERO) {
                    profitLoss
                        .divide(totalStaked, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal(100))
                } else {
                    BigDecimal.ZERO
                }
                
                PerformanceByProviderResponse(
                    providerId = providerId,
                    providerName = provider?.name ?: "Desconhecido",
                    totalBets = totalBets,
                    wins = wins,
                    losses = losses,
                    fullWins = fullWins,
                    partialWins = partialWins,
                    totalLosses = totalLosses,
                    partialLosses = partialLosses,
                    breakEven = breakEven,
                    winRate = winRate,
                    profitLoss = profitLoss,
                    roi = roi
                )
            }
    }
}

private fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
