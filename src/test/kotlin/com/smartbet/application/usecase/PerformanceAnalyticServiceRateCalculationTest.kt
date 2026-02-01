package com.smartbet.application.usecase

import com.smartbet.infrastructure.persistence.entity.*
import com.smartbet.infrastructure.persistence.repository.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Testes para verificar o cálculo correto de taxas no PerformanceAnalyticService.
 *
 * Este teste garante que winRate, successRate e ROI sejam calculados corretamente
 * mesmo quando os valores no banco de dados estão incorretos (zerados).
 */
class PerformanceAnalyticServiceRateCalculationTest {

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
    fun `should calculate correct winRate even when database value is zero`() {
        // Arrange: Bundesliga com 1 acerto em 1 aposta, mas win_rate zerado no banco
        val tournamentId = 1L
        val userId = 1L

        val performanceEntity = PerformanceByTournamentEntity(
            id = PerformanceByTournamentId(userId, tournamentId),
            totalTickets = 1,
            ticketsWon = 1,
            ticketsLost = 0,
            ticketsVoid = 0,
            ticketsFullWon = 1,     // 1 vitória completa
            ticketsPartialWon = 0,
            ticketsBreakEven = 0,
            ticketsPartialLost = 0,
            ticketsTotalLost = 0,
            totalStake = BigDecimal("10.00"),
            totalProfit = BigDecimal("35.59"),
            roi = BigDecimal("355.9000"),  // ROI correto
            winRate = BigDecimal.ZERO,     // PROBLEMA: taxa zerada no banco
            successRate = BigDecimal.ZERO, // PROBLEMA: taxa zerada no banco
            avgOdd = BigDecimal("4.559"),
            firstBetAt = System.currentTimeMillis(),
            lastSettledAt = System.currentTimeMillis()
        )

        val tournament = TournamentEntity(
            id = tournamentId,
            name = "Bundesliga",
            localName = "Alemanha"
        )

        every { byTournamentRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
        every { tournamentRepository.findAll() } returns listOf(tournament)

        // Act
        val result = service.getPerformanceByTournament(userId)

        // Assert
        assertEquals(1, result.size)
        val bundesliga = result[0]

        // Win rate deve ser 100% (1 vitória completa / 1 aposta total)
        assertEquals(BigDecimal("100.00"), bundesliga.winRate)

        // Success rate deve ser 100% (1 vitória total / 1 aposta total)
        assertEquals(BigDecimal("100.00"), bundesliga.successRate)

        // ROI deve ser recalculado corretamente
        assertEquals(BigDecimal("355.9000"), bundesliga.roi)
    }

    @Test
    fun `should calculate correct rates for Série A with 50 percent win rate`() {
        // Arrange: Série A com 1 acerto em 2 apostas
        val tournamentId = 2L
        val userId = 1L

        val performanceEntity = PerformanceByTournamentEntity(
            id = PerformanceByTournamentId(userId, tournamentId),
            totalTickets = 2,
            ticketsWon = 1,
            ticketsLost = 1,
            ticketsVoid = 0,
            ticketsFullWon = 1,
            ticketsPartialWon = 0,
            ticketsBreakEven = 0,
            ticketsPartialLost = 0,
            ticketsTotalLost = 1,
            totalStake = BigDecimal("20.00"),
            totalProfit = BigDecimal("17.04"),
            roi = BigDecimal.ZERO,         // ROI zerado no banco
            winRate = BigDecimal.ZERO,     // Taxa zerada no banco
            successRate = BigDecimal.ZERO, // Taxa zerada no banco
            avgOdd = BigDecimal("2.852"),
            firstBetAt = System.currentTimeMillis(),
            lastSettledAt = System.currentTimeMillis()
        )

        val tournament = TournamentEntity(
            id = tournamentId,
            name = "Série A",
            localName = "Itália"
        )

        every { byTournamentRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
        every { tournamentRepository.findAll() } returns listOf(tournament)

        // Act
        val result = service.getPerformanceByTournament(userId)

        // Assert
        assertEquals(1, result.size)
        val serieA = result[0]

        // Win rate deve ser 50% (1 vitória completa / 2 apostas)
        assertEquals(BigDecimal("50.00"), serieA.winRate)

        // Success rate deve ser 50% (1 vitória total / 2 apostas)
        assertEquals(BigDecimal("50.00"), serieA.successRate)

        // ROI deve ser 85.2% (17.04 / 20.00 * 100)
        assertEquals(BigDecimal("85.2000"), serieA.roi)
    }

    @Test
    fun `should calculate zero rates for Paulista with no wins`() {
        // Arrange: Paulista com 0 acertos em 1 aposta
        val tournamentId = 3L
        val userId = 1L

        val performanceEntity = PerformanceByTournamentEntity(
            id = PerformanceByTournamentId(userId, tournamentId),
            totalTickets = 1,
            ticketsWon = 0,
            ticketsLost = 1,
            ticketsVoid = 0,
            ticketsFullWon = 0,
            ticketsPartialWon = 0,
            ticketsBreakEven = 0,
            ticketsPartialLost = 0,
            ticketsTotalLost = 1,
            totalStake = BigDecimal("10.00"),
            totalProfit = BigDecimal("-0.51"),
            roi = BigDecimal.ZERO,         // ROI zerado no banco
            winRate = BigDecimal.ZERO,     // Taxa corretamente zerada
            successRate = BigDecimal.ZERO, // Taxa corretamente zerada
            avgOdd = BigDecimal("1.949"),
            firstBetAt = System.currentTimeMillis(),
            lastSettledAt = System.currentTimeMillis()
        )

        val tournament = TournamentEntity(
            id = tournamentId,
            name = "Paulista - Série A1",
            localName = "Brasil"
        )

        every { byTournamentRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
        every { tournamentRepository.findAll() } returns listOf(tournament)

        // Act
        val result = service.getPerformanceByTournament(userId)

        // Assert
        assertEquals(1, result.size)
        val paulista = result[0]

        // Win rate deve ser 0% (0 vitórias / 1 aposta)
        assertEquals(BigDecimal("0.00"), paulista.winRate)

        // Success rate deve ser 0% (0 vitórias / 1 aposta)
        assertEquals(BigDecimal("0.00"), paulista.successRate)

        // ROI deve ser -5.1% (-0.51 / 10.00 * 100)
        assertEquals(BigDecimal("-5.1000"), paulista.roi)
    }

    @Test
    fun `should handle partial wins correctly in success rate`() {
        // Arrange: Copa com 1 vitória parcial e 1 perda
        val tournamentId = 4L
        val userId = 1L

        val performanceEntity = PerformanceByTournamentEntity(
            id = PerformanceByTournamentId(userId, tournamentId),
            totalTickets = 2,
            ticketsWon = 1,           // ticketsWon = fullWon + partialWon
            ticketsLost = 1,
            ticketsVoid = 0,
            ticketsFullWon = 0,       // Nenhuma vitória completa
            ticketsPartialWon = 1,    // 1 vitória parcial (cashout ou sistema)
            ticketsBreakEven = 0,
            ticketsPartialLost = 0,
            ticketsTotalLost = 1,
            totalStake = BigDecimal("20.00"),
            totalProfit = BigDecimal("5.00"),
            roi = BigDecimal.ZERO,
            winRate = BigDecimal.ZERO,
            successRate = BigDecimal.ZERO,
            avgOdd = BigDecimal("2.500"),
            firstBetAt = System.currentTimeMillis(),
            lastSettledAt = System.currentTimeMillis()
        )

        val tournament = TournamentEntity(
            id = tournamentId,
            name = "Copa",
            localName = "França"
        )

        every { byTournamentRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
        every { tournamentRepository.findAll() } returns listOf(tournament)

        // Act
        val result = service.getPerformanceByTournament(userId)

        // Assert
        assertEquals(1, result.size)
        val copa = result[0]

        // Win rate deve ser 0% (0 vitórias COMPLETAS / 2 apostas)
        assertEquals(BigDecimal("0.00"), copa.winRate)

        // Success rate deve ser 50% (1 vitória parcial + 0 completas / 2 apostas)
        assertEquals(BigDecimal("50.00"), copa.successRate)

        // ROI deve ser 25% (5.00 / 20.00 * 100)
        assertEquals(BigDecimal("25.0000"), copa.roi)
    }
}
