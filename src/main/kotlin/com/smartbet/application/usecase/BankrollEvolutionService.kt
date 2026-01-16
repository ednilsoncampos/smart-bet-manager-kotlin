package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.repository.BankrollRepository
import com.smartbet.infrastructure.persistence.repository.BankrollTransactionRepository
import com.smartbet.infrastructure.persistence.repository.BetTicketRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class BankrollEvolutionService(
    private val bankrollRepository: BankrollRepository,
    private val transactionRepository: BankrollTransactionRepository,
    private val ticketRepository: BetTicketRepository,
    private val providerRepository: BettingProviderRepository
) {
    
    /**
     * Retorna a evolução de saldo de uma banca específica.
     */
    fun getBankrollEvolution(
        userId: Long,
        bankrollId: Long,
        params: EvolutionQueryParams = EvolutionQueryParams()
    ): BankrollEvolutionResponse {
        val bankroll = bankrollRepository.findById(bankrollId).orElseThrow {
            IllegalArgumentException("Banca não encontrada")
        }
        
        if (bankroll.userId != userId) {
            throw IllegalArgumentException("Banca não pertence ao usuário")
        }
        
        val provider = bankroll.providerId?.let { providerRepository.findById(it).orElse(null) }
        
        // Buscar transações da banca
        val transactions = transactionRepository.findByBankrollId(bankrollId)
            .sortedBy { it.createdAt }
        
        // Calcular evolução
        val evolution = calculateEvolution(transactions, params)
        
        val totalDeposited = bankroll.totalDeposited
        val currentBalance = bankroll.currentBalance
        val profitLoss = currentBalance - totalDeposited + bankroll.totalWithdrawn
        val roi = if (totalDeposited > BigDecimal.ZERO) {
            profitLoss.divide(totalDeposited, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        return BankrollEvolutionResponse(
            bankrollId = bankrollId,
            bankrollName = bankroll.name,
            providerName = provider?.name,
            currency = bankroll.currency,
            currentBalance = currentBalance.toDouble(),
            totalProfitLoss = profitLoss.toDouble(),
            overallRoi = roi.toDouble(),
            evolution = evolution
        )
    }
    
    /**
     * Retorna a evolução consolidada de todas as bancas do usuário.
     */
    fun getConsolidatedEvolution(
        userId: Long,
        params: EvolutionQueryParams = EvolutionQueryParams()
    ): ConsolidatedEvolutionResponse {
        val bankrolls = bankrollRepository.findByUserIdAndIsActiveTrue(userId)
        val providers = providerRepository.findAll().associateBy { it.id }
        
        // Consolidar por provider
        val byProvider = bankrolls.groupBy { it.providerId }.map { (providerId, providerBankrolls) ->
            val provider = providerId?.let { providers[it] }
            val totalBalance = providerBankrolls.sumOf { it.currentBalance }
            val totalDeposited = providerBankrolls.sumOf { it.totalDeposited }
            val totalWithdrawn = providerBankrolls.sumOf { it.totalWithdrawn }
            val profitLoss = totalBalance - totalDeposited + totalWithdrawn
            
            val roi = if (totalDeposited > BigDecimal.ZERO) {
                profitLoss.divide(totalDeposited, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
            
            ProviderEvolutionSummary(
                providerId = providerId ?: 0L,
                providerName = provider?.name ?: "Sem casa",
                currentBalance = totalBalance.toDouble(),
                profitLoss = profitLoss.toDouble(),
                roi = roi.toDouble()
            )
        }
        
        // Totais
        val totalCurrentBalance = bankrolls.sumOf { it.currentBalance }
        val totalDeposited = bankrolls.sumOf { it.totalDeposited }
        val totalWithdrawn = bankrolls.sumOf { it.totalWithdrawn }
        val totalProfitLoss = totalCurrentBalance - totalDeposited + totalWithdrawn
        
        val overallRoi = if (totalDeposited > BigDecimal.ZERO) {
            totalProfitLoss.divide(totalDeposited, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        // Calcular evolução consolidada baseada em bilhetes encerrados
        val evolution = calculateEvolutionFromTickets(userId, params)
        
        return ConsolidatedEvolutionResponse(
            totalCurrentBalance = totalCurrentBalance.toDouble(),
            totalProfitLoss = totalProfitLoss.toDouble(),
            overallRoi = overallRoi.toDouble(),
            evolution = evolution,
            byProvider = byProvider
        )
    }
    
    /**
     * Converte timestamp Long (milissegundos) para LocalDate.
     */
    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    }
    
    /**
     * Calcula a evolução baseada em transações.
     */
    private fun calculateEvolution(
        transactions: List<com.smartbet.infrastructure.persistence.entity.BankrollTransactionEntity>,
        params: EvolutionQueryParams
    ): List<BankrollEvolutionPoint> {
        if (transactions.isEmpty()) {
            return emptyList()
        }
        
        val zoneId = ZoneId.systemDefault()
        val points = mutableListOf<BankrollEvolutionPoint>()

        // Agrupar por dia
        val byDate = transactions.groupBy { tx ->
            tx.createdAt.toLocalDate(zoneId)
        }

        var runningBalance = BigDecimal.ZERO
        var totalDeposited = BigDecimal.ZERO
        
        val sortedDates = byDate.keys.sorted()
        
        for (date in sortedDates) {
            val dayTransactions = byDate[date] ?: continue
            
            for (tx in dayTransactions.sortedBy { it.createdAt }) {
                when (tx.type.name) {
                    "DEPOSIT", "BONUS" -> {
                        runningBalance += tx.amount
                        totalDeposited += tx.amount
                    }
                    "WITHDRAWAL" -> {
                        runningBalance -= tx.amount
                    }
                    "BET_WON" -> {
                        runningBalance += tx.amount
                    }
                    "BET_LOST" -> {
                        runningBalance -= tx.amount
                    }
                    "ADJUSTMENT" -> {
                        runningBalance += tx.amount
                    }
                }
            }
            
            val profitLoss = runningBalance - totalDeposited
            val roi = if (totalDeposited > BigDecimal.ZERO) {
                profitLoss.divide(totalDeposited, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
            
            points.add(
                BankrollEvolutionPoint(
                    date = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                    balance = runningBalance.toDouble(),
                    profitLoss = profitLoss.toDouble(),
                    roi = roi.toDouble()
                )
            )
        }
        
        return points
    }
    
    /**
     * Calcula a evolução consolidada baseada em bilhetes encerrados.
     */
    private fun calculateEvolutionFromTickets(
        userId: Long,
        params: EvolutionQueryParams
    ): List<BankrollEvolutionPoint> {
        val pageable = PageRequest.of(0, Int.MAX_VALUE)
        val tickets = ticketRepository.findByUserId(userId, pageable).content
            .filter { it.ticketStatus != TicketStatus.OPEN }
            .sortedBy { it.settledAt ?: it.createdAt }
        
        if (tickets.isEmpty()) {
            return emptyList()
        }
        
        val zoneId = ZoneId.systemDefault()
        val points = mutableListOf<BankrollEvolutionPoint>()

        val byDate = tickets.groupBy { ticket ->
            (ticket.settledAt ?: ticket.createdAt).toLocalDate(zoneId)
        }

        var totalStaked = BigDecimal.ZERO
        var totalReturns = BigDecimal.ZERO
        
        val sortedDates = byDate.keys.sorted()
        
        for (date in sortedDates) {
            val dayTickets = byDate[date] ?: continue
            
            for (ticket in dayTickets) {
                totalStaked += ticket.stake
                totalReturns += ticket.actualPayout ?: BigDecimal.ZERO
            }
            
            val profitLoss = totalReturns - totalStaked
            val roi = if (totalStaked > BigDecimal.ZERO) {
                profitLoss.divide(totalStaked, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
            
            points.add(
                BankrollEvolutionPoint(
                    date = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                    balance = profitLoss.toDouble(), // Lucro/Prejuízo acumulado
                    profitLoss = profitLoss.toDouble(),
                    roi = roi.toDouble()
                )
            )
        }
        
        return points
    }
    
    private fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal {
        var sum = BigDecimal.ZERO
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }
}
