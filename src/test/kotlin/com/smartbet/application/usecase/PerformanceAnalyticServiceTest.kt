package com.smartbet.application.usecase

import com.smartbet.infrastructure.persistence.entity.*
import com.smartbet.infrastructure.persistence.repository.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("PerformanceAnalyticService")
class PerformanceAnalyticServiceTest {

    private lateinit var overallRepository: PerformanceOverallRepository
    private lateinit var byProviderRepository: PerformanceByProviderRepository
    private lateinit var byMarketRepository: PerformanceByMarketRepository
    private lateinit var byMonthRepository: PerformanceByMonthRepository
    private lateinit var byTournamentRepository: PerformanceByTournamentRepository
    private lateinit var providerRepository: BettingProviderRepository
    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var selectionComponentRepository: BetSelectionComponentRepository
    private lateinit var ticketRepository: BetTicketRepository
    private lateinit var performanceAnalyticService: PerformanceAnalyticService

    @BeforeEach
    fun setup() {
        overallRepository = mockk()
        byProviderRepository = mockk()
        byMarketRepository = mockk()
        byMonthRepository = mockk()
        byTournamentRepository = mockk()
        providerRepository = mockk()
        tournamentRepository = mockk()
        selectionComponentRepository = mockk()
        ticketRepository = mockk()
        performanceAnalyticService = PerformanceAnalyticService(
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
    }

    @Nested
    @DisplayName("getOverallPerformance()")
    inner class GetOverallPerformanceTests {

        @Test
        @DisplayName("deve retornar performance completa quando existem dados")
        fun shouldReturnCompletePerformanceWhenDataExists() {
            val userId = 1L
            val performanceEntity = PerformanceOverallEntity(
                userId = userId,
                totalTickets = 100,
                ticketsWon = 60,
                ticketsLost = 35,
                ticketsVoid = 5,
                ticketsCashedOut = 2,
                totalStake = BigDecimal("10000.00"),
                totalReturn = BigDecimal("12500.00"),
                totalProfit = BigDecimal("2500.00"),
                roi = BigDecimal("25.0000"),
                winRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("2.5000"),
                avgStake = BigDecimal("100.00"),
                currentStreak = 3,
                bestWinStreak = 8,
                worstLossStreak = -5,
                biggestWin = BigDecimal("500.00"),
                biggestLoss = BigDecimal("-200.00"),
                bestRoiTicket = BigDecimal("150.0000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { overallRepository.findByUserId(userId) } returns performanceEntity

            val result = performanceAnalyticService.getOverallPerformance(userId)

            assertEquals(100L, result.totalBets)
            assertEquals(60L, result.wins)
            assertEquals(35L, result.losses)
            assertEquals(5L, result.voids)
            assertEquals(2L, result.cashedOut)
            assertEquals(BigDecimal("60.00"), result.winRate)
            assertEquals(BigDecimal("10000.00"), result.totalStaked)
            assertEquals(BigDecimal("12500.00"), result.totalReturns)
            assertEquals(BigDecimal("2500.00"), result.profitLoss)
            assertEquals(BigDecimal("25.0000"), result.roi)
            assertEquals(BigDecimal("2.5000"), result.avgOdd)
            assertEquals(BigDecimal("100.00"), result.avgStake)
            assertEquals(3, result.currentStreak)
            assertEquals(8, result.bestWinStreak)
            assertEquals(-5, result.worstLossStreak)
            assertEquals(BigDecimal("500.00"), result.biggestWin)
            assertEquals(BigDecimal("-200.00"), result.biggestLoss)
            assertEquals(BigDecimal("150.0000"), result.bestRoiTicket)
            assertEquals(1704067200000L, result.firstBetAt)
            assertEquals(1735689600000L, result.lastSettledAt)
        }

        @Test
        @DisplayName("deve retornar resposta vazia quando não há dados")
        fun shouldReturnEmptyResponseWhenNoData() {
            val userId = 1L

            every { overallRepository.findByUserId(userId) } returns null

            val result = performanceAnalyticService.getOverallPerformance(userId)

            assertEquals(0L, result.totalBets)
            assertEquals(0L, result.wins)
            assertEquals(0L, result.losses)
            assertEquals(0L, result.voids)
            assertEquals(0L, result.cashedOut)
            assertEquals(BigDecimal.ZERO, result.winRate)
            assertEquals(BigDecimal.ZERO, result.totalStaked)
            assertEquals(BigDecimal.ZERO, result.totalReturns)
            assertEquals(BigDecimal.ZERO, result.profitLoss)
            assertEquals(BigDecimal.ZERO, result.roi)
            assertNull(result.avgOdd)
            assertNull(result.avgStake)
            assertEquals(0, result.currentStreak)
            assertEquals(0, result.bestWinStreak)
            assertEquals(0, result.worstLossStreak)
            assertNull(result.biggestWin)
            assertNull(result.biggestLoss)
            assertNull(result.bestRoiTicket)
            assertNull(result.firstBetAt)
            assertEquals(0L, result.lastSettledAt)
        }
    }

    @Nested
    @DisplayName("getPerformanceByProvider()")
    inner class GetPerformanceByProviderTests {

        @Test
        @DisplayName("deve retornar performance por provider com dados completos")
        fun shouldReturnPerformanceByProviderWithCompleteData() {
            val userId = 1L
            val providerId = 1L
            val provider = BettingProviderEntity(
                id = providerId,
                name = "Superbet",
                slug = "superbet"
            )

            val performanceEntity = PerformanceByProviderEntity(
                id = PerformanceByProviderId(userId, providerId),
                totalTickets = 50,
                ticketsWon = 30,
                ticketsLost = 18,
                ticketsVoid = 2,
                ticketsCashedOut = 1,
                ticketsFullWon = 30,  // 30/50 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 2,   // voids = break even
                ticketsPartialLost = 0,
                ticketsTotalLost = 18,  // losses
                totalStake = BigDecimal("5000.00"),
                totalProfit = BigDecimal("1200.00"),
                roi = BigDecimal("24.0000"),
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("2.3000"),
                avgStake = BigDecimal("100.00"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { byProviderRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
            every { providerRepository.findAll() } returns listOf(provider)

            val result = performanceAnalyticService.getPerformanceByProvider(userId)

            assertEquals(1, result.size)
            val providerStats = result[0]

            assertEquals(providerId, providerStats.providerId)
            assertEquals("Superbet", providerStats.providerName)
            assertEquals(50L, providerStats.totalBets)
            assertEquals(30L, providerStats.wins)
            assertEquals(18L, providerStats.losses)
            assertEquals(2L, providerStats.voids)
            assertEquals(1L, providerStats.cashedOut)
            assertEquals(BigDecimal("60.00"), providerStats.winRate)
            assertEquals(BigDecimal("5000.00"), providerStats.totalStaked)
            assertEquals(BigDecimal("1200.00"), providerStats.profitLoss)
            assertEquals(BigDecimal("24.0000"), providerStats.roi)
            assertEquals(BigDecimal("2.3000"), providerStats.avgOdd)
            assertEquals(1704067200000L, providerStats.firstBetAt)
            assertEquals(1735689600000L, providerStats.lastSettledAt)
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há dados")
        fun shouldReturnEmptyListWhenNoData() {
            val userId = 1L

            every { byProviderRepository.findByIdUserId(userId) } returns emptyList()
            every { providerRepository.findAll() } returns emptyList()

            val result = performanceAnalyticService.getPerformanceByProvider(userId)

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("deve retornar múltiplos providers quando existem")
        fun shouldReturnMultipleProvidersWhenTheyExist() {
            val userId = 1L
            val provider1 = BettingProviderEntity(id = 1L, name = "Superbet", slug = "superbet")
            val provider2 = BettingProviderEntity(id = 2L, name = "Betano", slug = "betano")

            val performance1 = PerformanceByProviderEntity(
                id = PerformanceByProviderId(userId, 1L),
                totalTickets = 30,
                ticketsWon = 18,
                ticketsLost = 12,
                ticketsVoid = 0,
                ticketsCashedOut = 0,
                ticketsFullWon = 18,  // 18/30 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 12,
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("2.0000"),
                avgStake = BigDecimal("100.00"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            val performance2 = PerformanceByProviderEntity(
                id = PerformanceByProviderId(userId, 2L),
                totalTickets = 20,
                ticketsWon = 12,
                ticketsLost = 8,
                ticketsVoid = 0,
                ticketsCashedOut = 0,
                ticketsFullWon = 12,  // 12/20 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 8,
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("2.5000"),
                avgStake = BigDecimal("100.00"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { byProviderRepository.findByIdUserId(userId) } returns listOf(performance1, performance2)
            every { providerRepository.findAll() } returns listOf(provider1, provider2)

            val result = performanceAnalyticService.getPerformanceByProvider(userId)

            assertEquals(2, result.size)
            assertEquals("Superbet", result[0].providerName)
            assertEquals("Betano", result[1].providerName)
        }
    }

    @Nested
    @DisplayName("getPerformanceByMonth()")
    inner class GetPerformanceByMonthTests {

        @Test
        @DisplayName("deve retornar performance mensal com dados completos")
        fun shouldReturnPerformanceByMonthWithCompleteData() {
            val userId = 1L
            val year = 2026
            val month = 1

            val performanceEntity = PerformanceByMonthEntity(
                id = PerformanceByMonthId(userId, year, month),
                totalTickets = 25,
                ticketsWon = 15,
                ticketsLost = 9,
                ticketsVoid = 1,
                ticketsFullWon = 15,  // 15/25 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 1,
                ticketsPartialLost = 0,
                ticketsTotalLost = 9,
                totalStake = BigDecimal("2500.00"),
                totalProfit = BigDecimal("500.00"),
                roi = BigDecimal("20.0000"),
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgStake = BigDecimal("100.00"),
                avgOdd = BigDecimal("2.5000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1706745600000L
            )

            every { byMonthRepository.findByIdUserIdOrderByIdYearDescIdMonthDesc(userId) } returns listOf(performanceEntity)

            val result = performanceAnalyticService.getPerformanceByMonth(userId)

            assertEquals(1, result.size)
            val monthStats = result[0]

            assertEquals(year, monthStats.year)
            assertEquals(month, monthStats.month)
            assertEquals(25L, monthStats.totalBets)
            assertEquals(15L, monthStats.wins)
            assertEquals(9L, monthStats.losses)
            assertEquals(1L, monthStats.voids)
            assertEquals(BigDecimal("60.00"), monthStats.winRate)
            assertEquals(BigDecimal("2500.00"), monthStats.totalStaked)
            assertEquals(BigDecimal("500.00"), monthStats.profitLoss)
            assertEquals(BigDecimal("20.0000"), monthStats.roi)
            assertEquals(BigDecimal("100.00"), monthStats.avgStake)
            assertEquals(1704067200000L, monthStats.firstBetAt)
            assertEquals(1706745600000L, monthStats.lastSettledAt)
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há dados")
        fun shouldReturnEmptyListWhenNoData() {
            val userId = 1L

            every { byMonthRepository.findByIdUserIdOrderByIdYearDescIdMonthDesc(userId) } returns emptyList()

            val result = performanceAnalyticService.getPerformanceByMonth(userId)

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("deve retornar múltiplos meses ordenados do mais recente para o mais antigo")
        fun shouldReturnMultipleMonthsOrderedByRecentFirst() {
            val userId = 1L

            val jan2026 = PerformanceByMonthEntity(
                id = PerformanceByMonthId(userId, 2026, 1),
                totalTickets = 20,
                ticketsWon = 12,
                ticketsLost = 8,
                ticketsVoid = 0,
                ticketsFullWon = 12,  // 12/20 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 8,
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgStake = BigDecimal("100.00"),
                avgOdd = BigDecimal("2.5000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1706745600000L
            )

            val dec2025 = PerformanceByMonthEntity(
                id = PerformanceByMonthId(userId, 2025, 12),
                totalTickets = 30,
                ticketsWon = 18,
                ticketsLost = 12,
                ticketsVoid = 0,
                ticketsFullWon = 18,  // 18/30 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 12,
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgStake = BigDecimal("100.00"),
                avgOdd = BigDecimal("2.5000"),
                firstBetAt = 1701475200000L,
                lastSettledAt = 1704067200000L
            )

            val nov2025 = PerformanceByMonthEntity(
                id = PerformanceByMonthId(userId, 2025, 11),
                totalTickets = 15,
                ticketsWon = 9,
                ticketsLost = 6,
                ticketsVoid = 0,
                ticketsFullWon = 9,  // 9/15 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 6,
                totalStake = BigDecimal("1500.00"),
                totalProfit = BigDecimal("300.00"),
                roi = BigDecimal("20.0000"),
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgStake = BigDecimal("100.00"),
                avgOdd = BigDecimal("2.5000"),
                firstBetAt = 1698883200000L,
                lastSettledAt = 1701475200000L
            )

            // Repository já retorna ordenado DESC
            every { byMonthRepository.findByIdUserIdOrderByIdYearDescIdMonthDesc(userId) } returns listOf(jan2026, dec2025, nov2025)

            val result = performanceAnalyticService.getPerformanceByMonth(userId)

            assertEquals(3, result.size)
            // Verifica ordem: Jan/2026, Dez/2025, Nov/2025
            assertEquals(2026, result[0].year)
            assertEquals(1, result[0].month)
            assertEquals(2025, result[1].year)
            assertEquals(12, result[1].month)
            assertEquals(2025, result[2].year)
            assertEquals(11, result[2].month)
        }

        @Test
        @DisplayName("deve incluir avgStake null quando não há dados suficientes")
        fun shouldIncludeNullAvgStakeWhenNotEnoughData() {
            val userId = 1L

            val performanceEntity = PerformanceByMonthEntity(
                id = PerformanceByMonthId(userId, 2026, 1),
                totalTickets = 1,
                ticketsWon = 1,
                ticketsLost = 0,
                ticketsVoid = 0,
                ticketsFullWon = 1,
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 0,
                totalStake = BigDecimal("100.00"),
                totalProfit = BigDecimal("50.00"),
                roi = BigDecimal("50.0000"),
                winRate = BigDecimal("100.00"),
                successRate = BigDecimal("100.00"),
                avgStake = null, // Sem avgStake
                avgOdd = null,
                firstBetAt = 1704067200000L,
                lastSettledAt = 1704067200000L
            )

            every { byMonthRepository.findByIdUserIdOrderByIdYearDescIdMonthDesc(userId) } returns listOf(performanceEntity)

            val result = performanceAnalyticService.getPerformanceByMonth(userId)

            assertEquals(1, result.size)
            assertNull(result[0].avgStake)
        }
    }

    @Nested
    @DisplayName("getPerformanceByMarket()")
    inner class GetPerformanceByMarketTests {

        @Test
        @DisplayName("deve retornar performance por mercado com dados completos")
        fun shouldReturnPerformanceByMarketWithCompleteData() {
            val userId = 1L
            val marketType = "Resultado Final"

            val performanceEntity = PerformanceByMarketEntity(
                id = PerformanceByMarketId(userId, marketType),
                totalSelections = 120,
                wins = 75,
                losses = 40,
                voids = 5,
                uniqueTickets = 80,
                winRate = BigDecimal("62.50"),
                avgOdd = BigDecimal("2.2000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { byMarketRepository.findByIdUserId(userId) } returns listOf(performanceEntity)

            val result = performanceAnalyticService.getPerformanceByMarket(userId)

            assertEquals(1, result.size)
            val marketStats = result[0]

            assertEquals(marketType, marketStats.marketType)
            assertEquals(120L, marketStats.totalSelections)
            assertEquals(80L, marketStats.uniqueTickets)
            assertEquals(75L, marketStats.wins)
            assertEquals(40L, marketStats.losses)
            assertEquals(5L, marketStats.voids)
            assertEquals(BigDecimal("62.50"), marketStats.winRate)
            assertEquals(BigDecimal("2.2000"), marketStats.avgOdd)
            assertEquals(1704067200000L, marketStats.firstBetAt)
            assertEquals(1735689600000L, marketStats.lastSettledAt)
            assertNull(marketStats.betBuilderComponents)
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há dados")
        fun shouldReturnEmptyListWhenNoData() {
            val userId = 1L

            every { byMarketRepository.findByIdUserId(userId) } returns emptyList()

            val result = performanceAnalyticService.getPerformanceByMarket(userId)

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("deve buscar componentes de Bet Builder quando mercado é 'Criar Aposta'")
        fun shouldFetchBetBuilderComponentsWhenMarketIsCriarAposta() {
            val userId = 1L
            val marketType = "Criar Aposta"

            val performanceEntity = PerformanceByMarketEntity(
                id = PerformanceByMarketId(userId, marketType),
                totalSelections = 50,
                wins = 30,
                losses = 20,
                voids = 0,
                uniqueTickets = 25,
                winRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("3.0000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { byMarketRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
            every { selectionComponentRepository.findByUserId(userId) } returns emptyList()

            val result = performanceAnalyticService.getPerformanceByMarket(userId)

            assertEquals(1, result.size)
            assertNotNull(result[0].betBuilderComponents)
            assertTrue(result[0].betBuilderComponents!!.isEmpty())
        }
    }

    @Nested
    @DisplayName("getPerformanceByTournament()")
    inner class GetPerformanceByTournamentTests {

        @Test
        @DisplayName("deve retornar performance por torneio com dados completos")
        fun shouldReturnPerformanceByTournamentWithCompleteData() {
            val userId = 1L
            val tournamentId = 10L
            val tournament = TournamentEntity(
                id = tournamentId,
                name = "Premier League",
                localName = "Inglaterra",
                providerId = 1L,
                sportId = 5L,
                externalId = 100
            )

            val performanceEntity = PerformanceByTournamentEntity(
                id = PerformanceByTournamentId(userId, tournamentId),
                totalTickets = 40,
                ticketsWon = 25,
                ticketsLost = 14,
                ticketsVoid = 1,
                ticketsFullWon = 25,  // 25/40 * 100 = 62.50% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 1,
                ticketsPartialLost = 0,
                ticketsTotalLost = 14,
                winRate = BigDecimal("62.50"),
                successRate = BigDecimal("62.50"),
                avgOdd = BigDecimal("2.1000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { byTournamentRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
            every { tournamentRepository.findAll() } returns listOf(tournament)

            val result = performanceAnalyticService.getPerformanceByTournament(userId)

            assertEquals(1, result.size)
            val tournamentStats = result[0]

            assertEquals(tournamentId, tournamentStats.tournamentId)
            assertEquals("Premier League", tournamentStats.tournamentName)
            assertEquals("Inglaterra", tournamentStats.tournamentLocalName)
            assertEquals(40L, tournamentStats.totalBets)
            assertEquals(25L, tournamentStats.wins)
            assertEquals(14L, tournamentStats.losses)
            assertEquals(1L, tournamentStats.voids)
            assertEquals(BigDecimal("62.50"), tournamentStats.winRate)
            assertEquals(BigDecimal("2.1000"), tournamentStats.avgOdd)
            assertEquals(1704067200000L, tournamentStats.firstBetAt)
            assertEquals(1735689600000L, tournamentStats.lastSettledAt)
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há dados")
        fun shouldReturnEmptyListWhenNoData() {
            val userId = 1L

            every { byTournamentRepository.findByIdUserId(userId) } returns emptyList()
            every { tournamentRepository.findAll() } returns emptyList()

            val result = performanceAnalyticService.getPerformanceByTournament(userId)

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("deve retornar múltiplos torneios")
        fun shouldReturnMultipleTournaments() {
            val userId = 1L
            val tournament1 = TournamentEntity(
                id = 1L,
                name = "Premier League",
                localName = "Inglaterra",
                providerId = 1L,
                sportId = 5L,
                externalId = 100
            )
            val tournament2 = TournamentEntity(
                id = 2L,
                name = "La Liga",
                localName = "Espanha",
                providerId = 1L,
                sportId = 5L,
                externalId = 200
            )

            val performance1 = PerformanceByTournamentEntity(
                id = PerformanceByTournamentId(userId, 1L),
                totalTickets = 30,
                ticketsWon = 18,
                ticketsLost = 12,
                ticketsVoid = 0,
                ticketsFullWon = 18,  // 18/30 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 12,
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("2.0000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            val performance2 = PerformanceByTournamentEntity(
                id = PerformanceByTournamentId(userId, 2L),
                totalTickets = 20,
                ticketsWon = 12,
                ticketsLost = 8,
                ticketsVoid = 0,
                ticketsFullWon = 12,  // 12/20 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 8,
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("2.5000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { byTournamentRepository.findByIdUserId(userId) } returns listOf(performance1, performance2)
            every { tournamentRepository.findAll() } returns listOf(tournament1, tournament2)

            val result = performanceAnalyticService.getPerformanceByTournament(userId)

            assertEquals(2, result.size)
            assertEquals("Premier League", result[0].tournamentName)
            assertEquals("La Liga", result[1].tournamentName)
        }

        @Test
        @DisplayName("deve retornar 'Torneio Desconhecido' quando torneio não existe")
        fun shouldReturnUnknownTournamentWhenTournamentNotFound() {
            val userId = 1L
            val tournamentId = 999L

            val performanceEntity = PerformanceByTournamentEntity(
                id = PerformanceByTournamentId(userId, tournamentId),
                totalTickets = 10,
                ticketsWon = 6,
                ticketsLost = 4,
                ticketsVoid = 0,
                ticketsFullWon = 6,  // 6/10 * 100 = 60% winRate
                ticketsPartialWon = 0,
                ticketsBreakEven = 0,
                ticketsPartialLost = 0,
                ticketsTotalLost = 4,
                winRate = BigDecimal("60.00"),
                successRate = BigDecimal("60.00"),
                avgOdd = BigDecimal("2.0000"),
                firstBetAt = 1704067200000L,
                lastSettledAt = 1735689600000L
            )

            every { byTournamentRepository.findByIdUserId(userId) } returns listOf(performanceEntity)
            every { tournamentRepository.findAll() } returns emptyList()

            val result = performanceAnalyticService.getPerformanceByTournament(userId)

            assertEquals(1, result.size)
            assertEquals("Torneio Desconhecido", result[0].tournamentName)
            assertNull(result[0].tournamentLocalName)
        }
    }
}
