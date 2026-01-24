package com.smartbet.application.usecase

import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
import com.smartbet.infrastructure.persistence.entity.BettingProviderEntity
import com.smartbet.infrastructure.persistence.repository.BetSelectionRepository
import com.smartbet.infrastructure.persistence.repository.BetTicketRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

@DisplayName("PerformanceAnalyticService")
class PerformanceAnalyticServiceTest {
    
    private lateinit var ticketRepository: BetTicketRepository
    private lateinit var selectionRepository: BetSelectionRepository
    private lateinit var providerRepository: BettingProviderRepository
    private lateinit var performanceAnalyticService: PerformanceAnalyticService
    
    @BeforeEach
    fun setup() {
        ticketRepository = mockk()
        selectionRepository = mockk()
        providerRepository = mockk()
        performanceAnalyticService = PerformanceAnalyticService(ticketRepository, selectionRepository, providerRepository)
    }
    
    private fun createTicket(
        id: Long,
        userId: Long = 1L,
        providerId: Long = 1L,
        stake: BigDecimal,
        totalOdd: BigDecimal,
        actualPayout: BigDecimal?,
        ticketStatus: TicketStatus,
        financialStatus: FinancialStatus,
        profitLoss: BigDecimal
    ): BetTicketEntity {
        return BetTicketEntity(
            id = id,
            userId = userId,
            providerId = providerId,
            bankrollId = null,
            externalTicketId = "EXT-$id",
            sourceUrl = "https://example.com/$id",
            betType = BetType.SINGLE,
            stake = stake,
            totalOdd = totalOdd,
            potentialPayout = stake.multiply(totalOdd),
            actualPayout = actualPayout,
            ticketStatus = ticketStatus,
            financialStatus = financialStatus,
            profitLoss = profitLoss,
            roi = if (stake > BigDecimal.ZERO) profitLoss.divide(stake, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal(100)) else BigDecimal.ZERO
        )
    }
    
    @Nested
    @DisplayName("getOverallPerformance()")
    inner class GetOverallPerformanceTests {
        
        @Test
        @DisplayName("deve calcular mediana corretamente com número ímpar de elementos")
        fun shouldCalculateMedianWithOddNumberOfElements() {
            val tickets = listOf(
                createTicket(1, stake = BigDecimal("100"), totalOdd = BigDecimal("1.50"), actualPayout = BigDecimal("150"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("50")),
                createTicket(2, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("200"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("100")),
                createTicket(3, stake = BigDecimal("100"), totalOdd = BigDecimal("3.00"), actualPayout = BigDecimal("300"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("200"))
            )
            
            every { ticketRepository.findByUserId(any(), any<Pageable>()) } returns PageImpl(tickets)
            
            val result = performanceAnalyticService.getOverallPerformance(1L)
            
            // Mediana de [1.50, 2.00, 3.00] = 2.00 (elemento central)
            assertEquals(BigDecimal("2.00"), result.medianOdd.setScale(2))
        }
        
        @Test
        @DisplayName("deve calcular mediana corretamente com número par de elementos")
        fun shouldCalculateMedianWithEvenNumberOfElements() {
            val tickets = listOf(
                createTicket(1, stake = BigDecimal("100"), totalOdd = BigDecimal("1.50"), actualPayout = BigDecimal("150"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("50")),
                createTicket(2, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("200"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("100")),
                createTicket(3, stake = BigDecimal("100"), totalOdd = BigDecimal("3.00"), actualPayout = BigDecimal("300"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("200")),
                createTicket(4, stake = BigDecimal("100"), totalOdd = BigDecimal("4.00"), actualPayout = BigDecimal("400"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("300"))
            )
            
            every { ticketRepository.findByUserId(any(), any<Pageable>()) } returns PageImpl(tickets)
            
            val result = performanceAnalyticService.getOverallPerformance(1L)
            
            // Mediana de [1.50, 2.00, 3.00, 4.00] = (2.00 + 3.00) / 2 = 2.50
            assertEquals(BigDecimal("2.50"), result.medianOdd.setScale(2))
        }
        
        @Test
        @DisplayName("deve retornar zero quando não há bilhetes")
        fun shouldReturnZeroWhenNoTickets() {
            every { ticketRepository.findByUserId(any(), any<Pageable>()) } returns PageImpl(emptyList())
            
            val result = performanceAnalyticService.getOverallPerformance(1L)
            
            assertEquals(BigDecimal.ZERO, result.medianOdd)
            assertEquals(0L, result.totalBets)
        }
        
        @Test
        @DisplayName("deve ser resistente a outliers (odds muito altas)")
        fun shouldBeResistantToOutliers() {
            val tickets = listOf(
                createTicket(1, stake = BigDecimal("100"), totalOdd = BigDecimal("1.50"), actualPayout = BigDecimal("150"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("50")),
                createTicket(2, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("200"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("100")),
                createTicket(3, stake = BigDecimal("100"), totalOdd = BigDecimal("2.50"), actualPayout = BigDecimal("250"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("150")),
                createTicket(4, stake = BigDecimal("100"), totalOdd = BigDecimal("100.00"), actualPayout = BigDecimal("10000"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("9900")) // Outlier
            )
            
            every { ticketRepository.findByUserId(any(), any<Pageable>()) } returns PageImpl(tickets)
            
            val result = performanceAnalyticService.getOverallPerformance(1L)
            
            // Mediana de [1.50, 2.00, 2.50, 100.00] = (2.00 + 2.50) / 2 = 2.25
            // A média seria (1.50 + 2.00 + 2.50 + 100.00) / 4 = 26.50 (distorcida pelo outlier)
            assertEquals(BigDecimal("2.25"), result.medianOdd.setScale(2))
        }
        
        @Test
        @DisplayName("deve contar todos os status financeiros detalhados")
        fun shouldCountAllDetailedFinancialStatuses() {
            val tickets = listOf(
                createTicket(1, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("200"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("100")),
                createTicket(2, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("150"), ticketStatus = TicketStatus.PARTIAL_WIN, financialStatus = FinancialStatus.PARTIAL_WIN, profitLoss = BigDecimal("50")),
                createTicket(3, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("100"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.BREAK_EVEN, profitLoss = BigDecimal("0")),
                createTicket(4, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("80"), ticketStatus = TicketStatus.PARTIAL_LOSS, financialStatus = FinancialStatus.PARTIAL_LOSS, profitLoss = BigDecimal("-20")),
                createTicket(5, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("0"), ticketStatus = TicketStatus.LOST, financialStatus = FinancialStatus.TOTAL_LOSS, profitLoss = BigDecimal("-100")),
                createTicket(6, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = null, ticketStatus = TicketStatus.OPEN, financialStatus = FinancialStatus.PENDING, profitLoss = BigDecimal("0"))
            )
            
            every { ticketRepository.findByUserId(any(), any<Pageable>()) } returns PageImpl(tickets)
            
            val result = performanceAnalyticService.getOverallPerformance(1L)
            
            assertEquals(6L, result.totalBets)
            assertEquals(5L, result.settledBets)
            assertEquals(1L, result.openBets)
            assertEquals(1L, result.fullWins)
            assertEquals(1L, result.partialWins)
            assertEquals(1L, result.breakEven)
            assertEquals(1L, result.partialLosses)
            assertEquals(1L, result.totalLosses)
            assertEquals(2L, result.wins) // fullWins + partialWins
            assertEquals(2L, result.losses) // totalLosses + partialLosses
        }
    }
    
    @Nested
    @DisplayName("getPerformanceByProvider()")
    inner class GetPerformanceByProviderTests {
        
        @Test
        @DisplayName("deve contar todos os status financeiros corretamente")
        fun shouldCountAllFinancialStatusesCorrectly() {
            val provider = BettingProviderEntity(id = 1L, name = "Superbet", slug = "superbet")
            
            val tickets = listOf(
                createTicket(1, providerId = 1L, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("200"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("100")),
                createTicket(2, providerId = 1L, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("150"), ticketStatus = TicketStatus.PARTIAL_WIN, financialStatus = FinancialStatus.PARTIAL_WIN, profitLoss = BigDecimal("50")),
                createTicket(3, providerId = 1L, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("0"), ticketStatus = TicketStatus.LOST, financialStatus = FinancialStatus.TOTAL_LOSS, profitLoss = BigDecimal("-100")),
                createTicket(4, providerId = 1L, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("80"), ticketStatus = TicketStatus.PARTIAL_LOSS, financialStatus = FinancialStatus.PARTIAL_LOSS, profitLoss = BigDecimal("-20")),
                createTicket(5, providerId = 1L, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("100"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.BREAK_EVEN, profitLoss = BigDecimal("0"))
            )
            
            every { ticketRepository.findByUserId(any(), any<Pageable>()) } returns PageImpl(tickets)
            every { providerRepository.findAll() } returns listOf(provider)
            
            val result = performanceAnalyticService.getPerformanceByProvider(1L)
            
            assertEquals(1, result.size)
            val providerStats = result[0]
            
            assertEquals(5L, providerStats.totalBets)
            assertEquals(2L, providerStats.wins) // FULL_WIN + PARTIAL_WIN
            assertEquals(2L, providerStats.losses) // TOTAL_LOSS + PARTIAL_LOSS
            assertEquals(1L, providerStats.fullWins)
            assertEquals(1L, providerStats.partialWins)
            assertEquals(1L, providerStats.totalLosses)
            assertEquals(1L, providerStats.partialLosses)
            assertEquals(1L, providerStats.breakEven)
        }
        
        @Test
        @DisplayName("não deve contar bilhetes em aberto")
        fun shouldNotCountOpenTickets() {
            val provider = BettingProviderEntity(id = 1L, name = "Superbet", slug = "superbet")
            
            val tickets = listOf(
                createTicket(1, providerId = 1L, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = BigDecimal("200"), ticketStatus = TicketStatus.WON, financialStatus = FinancialStatus.FULL_WIN, profitLoss = BigDecimal("100")),
                createTicket(2, providerId = 1L, stake = BigDecimal("100"), totalOdd = BigDecimal("2.00"), actualPayout = null, ticketStatus = TicketStatus.OPEN, financialStatus = FinancialStatus.PENDING, profitLoss = BigDecimal("0"))
            )
            
            every { ticketRepository.findByUserId(any(), any<Pageable>()) } returns PageImpl(tickets)
            every { providerRepository.findAll() } returns listOf(provider)
            
            val result = performanceAnalyticService.getPerformanceByProvider(1L)
            
            assertEquals(1, result.size)
            assertEquals(1L, result[0].totalBets) // Apenas o bilhete WON
        }
    }
}
