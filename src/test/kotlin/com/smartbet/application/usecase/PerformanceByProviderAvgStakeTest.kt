package com.smartbet.application.usecase

import com.smartbet.infrastructure.persistence.entity.*
import com.smartbet.infrastructure.persistence.repository.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Testes para verificar o cálculo correto de avgStake no endpoint by-provider.
 */
class PerformanceByProviderAvgStakeTest {

    private val overallRepository = mockk<PerformanceOverallRepository>()
    private val byProviderRepository = mockk<PerformanceByProviderRepository>()
    private val byMarketRepository = mockk<PerformanceByMarketRepository>()
    private val byMonthRepository = mockk<PerformanceByMonthRepository>()
    private val byTournamentRepository = mockk<PerformanceByTournamentRepository>()
    private val providerRepository = mockk<BettingProviderRepository>()
    private val tournamentRepository = mockk<TournamentRepository>()
    private val selectionComponentRepository = mockk<BetSelectionComponentRepository>()
    private val ticketRepository = mockk<BetTicketRepository>()

    private val service = PerformanceAnalyticService(
        overallRepository,
        byProviderRepository,
        byMarketRepository,
        byMonthRepository,
        byTournamentRepository,
        providerRepository,
        tournamentRepository,
        selectionComponentRepository,
        ticketRepository
    )

    @Test
    fun `should return avgStake in performance by provider`() {
        // Arrange
        val providerId = 1L
        val userId = 1L

        val performanceEntity = PerformanceByProviderEntity(
            id = PerformanceByProviderId(userId, providerId),
            totalTickets = 5,
            ticketsWon = 3,
            ticketsLost = 2,
            ticketsVoid = 0,
            ticketsCashedOut = 0,
            ticketsFullWon = 2,
            ticketsPartialWon = 1,
            ticketsBreakEven = 0,
            ticketsPartialLost = 0,
            ticketsTotalLost = 2,
            totalStake = BigDecimal("50.00"),    // Total apostado
            totalProfit = BigDecimal("25.00"),
            roi = BigDecimal.ZERO,
            winRate = BigDecimal.ZERO,
            successRate = BigDecimal.ZERO,
            avgOdd = BigDecimal("2.50"),
            avgStake = BigDecimal("10.00"),       // Média: 50 / 5 = 10
            firstBetAt = System.currentTimeMillis(),
            lastSettledAt = System.currentTimeMillis()
        )

        val provider = BettingProviderEntity(
            id = providerId,
            name = "Betano",
            baseUrl = "https://betano.com"
        )

        every { byProviderRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
        every { providerRepository.findAll() } returns listOf(provider)

        // Act
        val result = service.getPerformanceByProvider(userId)

        // Assert
        assertEquals(1, result.size)
        val betano = result[0]

        // Verifica que avgStake está presente e correto
        assertEquals(BigDecimal("10.00"), betano.avgStake)
        assertEquals("Betano", betano.providerName)
        assertEquals(5L, betano.totalBets)
        assertEquals(BigDecimal("50.00"), betano.totalStaked)
    }

    @Test
    fun `should return all required fields for Por Casa screen`() {
        // Arrange
        val providerId = 2L
        val userId = 1L

        val performanceEntity = PerformanceByProviderEntity(
            id = PerformanceByProviderId(userId, providerId),
            totalTickets = 10,
            ticketsWon = 6,
            ticketsLost = 4,
            ticketsVoid = 0,
            ticketsCashedOut = 2,
            ticketsFullWon = 4,
            ticketsPartialWon = 2,
            ticketsBreakEven = 0,
            ticketsPartialLost = 1,
            ticketsTotalLost = 3,
            totalStake = BigDecimal("100.00"),
            totalProfit = BigDecimal("35.00"),
            roi = BigDecimal.ZERO,
            winRate = BigDecimal.ZERO,
            successRate = BigDecimal.ZERO,
            avgOdd = BigDecimal("3.25"),
            avgStake = BigDecimal("10.00"),
            firstBetAt = System.currentTimeMillis(),
            lastSettledAt = System.currentTimeMillis()
        )

        val provider = BettingProviderEntity(
            id = providerId,
            name = "Superbet",
            baseUrl = "https://superbet.com"
        )

        every { byProviderRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
        every { providerRepository.findAll() } returns listOf(provider)

        // Act
        val result = service.getPerformanceByProvider(userId)

        // Assert
        assertEquals(1, result.size)
        val superbet = result[0]

        // Verifica TODOS os campos solicitados para a tela "Por Casa"
        assertEquals(BigDecimal("10.00"), superbet.avgStake)      // ✅ Stake média
        assertEquals(BigDecimal("3.25"), superbet.avgOdd)         // ✅ Odd média
        assertEquals(BigDecimal("60.00"), superbet.successRate)   // ✅ Taxa de sucesso (6/10 * 100)
        assertEquals(BigDecimal("35.0000"), superbet.roi)         // ✅ ROI (35/100 * 100)
        assertEquals(2L, superbet.partialWins)                    // ✅ Ganhos parciais
        assertEquals(1L, superbet.partialLosses)                  // ✅ Perdas parciais

        // Outros campos importantes
        assertEquals("Superbet", superbet.providerName)
        assertEquals(10L, superbet.totalBets)
        assertEquals(BigDecimal("100.00"), superbet.totalStaked)
        assertEquals(BigDecimal("35.00"), superbet.profitLoss)
    }

    @Test
    fun `should handle null avgStake when no tickets exist`() {
        // Arrange
        val userId = 1L

        every { byProviderRepository.findByIdUserId(userId) } returns emptyList()
        every { providerRepository.findAll() } returns emptyList()

        // Act
        val result = service.getPerformanceByProvider(userId)

        // Assert
        assertEquals(0, result.size)
    }
}
