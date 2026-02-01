package com.smartbet.application.usecase

import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.event.TicketSettledEvent
import com.smartbet.infrastructure.persistence.entity.PerformanceByMarketId
import com.smartbet.infrastructure.persistence.repository.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Testes para validar a divisão proporcional de stake/profit por mercado.
 *
 * A lógica de divisão proporcional garante que a soma dos stakes de todos
 * os mercados seja igual ao stake total do ticket, evitando inflação de valores.
 */
class ProportionalDivisionByMarketTest {

    private lateinit var service: AnalyticsAggregationService
    private lateinit var byMarketRepository: PerformanceByMarketRepository
    private lateinit var overallRepository: PerformanceOverallRepository
    private lateinit var byMonthRepository: PerformanceByMonthRepository
    private lateinit var byProviderRepository: PerformanceByProviderRepository
    private lateinit var byTournamentRepository: PerformanceByTournamentRepository

    @BeforeEach
    fun setup() {
        overallRepository = mockk(relaxed = true)
        byMonthRepository = mockk(relaxed = true)
        byProviderRepository = mockk(relaxed = true)
        byMarketRepository = mockk(relaxed = true)
        byTournamentRepository = mockk(relaxed = true)

        // Configure save methods to return the argument they receive
        every { overallRepository.save(any()) } answers { firstArg() }
        every { byMonthRepository.save(any()) } answers { firstArg() }
        every { byProviderRepository.save(any()) } answers { firstArg() }
        every { byTournamentRepository.save(any()) } answers { firstArg() }

        service = AnalyticsAggregationService(
            overallRepository,
            byMonthRepository,
            byProviderRepository,
            byMarketRepository,
            byTournamentRepository
        )
    }

    @Test
    fun `should divide stake proportionally when ticket has 3 selections in 2 different markets`() {
        // Given: Ticket com 3 seleções em 2 mercados
        // - 2x Handicap
        // - 1x Total de Gols
        // Stake: R$ 30, Profit: R$ 15
        val event = TicketSettledEvent(
            ticketId = 1L,
            userId = 1L,
            providerId = 1L,
            stake = BigDecimal("30.00"),
            totalOdd = BigDecimal("2.50"),
            actualPayout = BigDecimal("45.00"),
            profitLoss = BigDecimal("15.00"),
            roi = BigDecimal("50.00"),
            ticketStatus = com.smartbet.domain.enum.TicketStatus.WIN,
            financialStatus = FinancialStatus.FULL_WIN,
            settledAt = System.currentTimeMillis(),
            selections = listOf(
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = 1L,
                    status = SelectionStatus.WON,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = 2L,
                    status = SelectionStatus.WON,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Total de Gols",
                    tournamentId = 3L,
                    status = SelectionStatus.WON,
                    eventDate = null
                )
            )
        )

        val capturedEntities = mutableListOf<com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity>()
        every { byMarketRepository.findByIdUserIdAndIdMarketType(any(), any()) } returns null
        every { byMarketRepository.save(capture(capturedEntities)) } answers { firstArg() }

        // When
        service.updateOnSettlement(event)

        // Then: Verifica que foram criados 2 registros (1 para cada mercado)
        verify(exactly = 2) { byMarketRepository.save(any()) }

        // Encontra entidades por mercado
        val handicapEntity = capturedEntities.find { it.id.marketType == "Handicap" }
        val totalGolsEntity = capturedEntities.find { it.id.marketType == "Total de Gols" }

        assertNotNull(handicapEntity, "Deve criar registro para Handicap")
        assertNotNull(totalGolsEntity, "Deve criar registro para Total de Gols")

        // Handicap: 2 seleções de 3 total = 2/3 = 0.6667
        // Stake: R$ 30 × 0.6667 = R$ 20.00
        // Profit: R$ 15 × 0.6667 = R$ 10.00
        assertEquals(BigDecimal("20.00"), handicapEntity!!.totalStake, "Handicap deve ter 2/3 do stake")
        assertEquals(BigDecimal("10.00"), handicapEntity.totalProfit, "Handicap deve ter 2/3 do profit")
        assertEquals(BigDecimal("50.00"), handicapEntity.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve ser recalculado corretamente")

        // Total de Gols: 1 seleção de 3 total = 1/3 = 0.3333
        // Stake: R$ 30 × 0.3333 = R$ 10.00
        // Profit: R$ 15 × 0.3333 = R$ 5.00
        assertEquals(BigDecimal("10.00"), totalGolsEntity!!.totalStake, "Total de Gols deve ter 1/3 do stake")
        assertEquals(BigDecimal("5.00"), totalGolsEntity.totalProfit, "Total de Gols deve ter 1/3 do profit")
        assertEquals(BigDecimal("50.00"), totalGolsEntity.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve ser recalculado corretamente")

        // Valida que a soma dos stakes = stake total do ticket
        val totalStake = handicapEntity.totalStake.add(totalGolsEntity.totalStake)
        assertEquals(BigDecimal("30.00"), totalStake, "Soma dos stakes deve ser igual ao stake total do ticket")

        // Valida que a soma dos profits = profit total do ticket
        val totalProfit = handicapEntity.totalProfit.add(totalGolsEntity.totalProfit)
        assertEquals(BigDecimal("15.00"), totalProfit, "Soma dos profits deve ser igual ao profit total do ticket")
    }

    @Test
    fun `should use full stake when ticket has only 1 selection in 1 market`() {
        // Given: Ticket com 1 seleção em 1 mercado
        // Stake: R$ 15, Profit: R$ 30
        val event = TicketSettledEvent(
            ticketId = 2L,
            userId = 1L,
            providerId = 1L,
            stake = BigDecimal("15.00"),
            totalOdd = BigDecimal("3.00"),
            actualPayout = BigDecimal("45.00"),
            profitLoss = BigDecimal("30.00"),
            roi = BigDecimal("200.00"),
            ticketStatus = com.smartbet.domain.enum.TicketStatus.WIN,
            financialStatus = FinancialStatus.FULL_WIN,
            settledAt = System.currentTimeMillis(),
            selections = listOf(
                TicketSettledEvent.SelectionData(
                    marketType = "1X2",
                    tournamentId = 1L,
                    status = SelectionStatus.WON,
                    eventDate = null
                )
            )
        )

        val capturedEntity = slot<com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity>()
        every { byMarketRepository.findByIdUserIdAndIdMarketType(any(), any()) } returns null
        every { byMarketRepository.save(capture(capturedEntity)) } answers { firstArg() }

        // When
        service.updateOnSettlement(event)

        // Then: Verifica que foi criado 1 registro
        verify(exactly = 1) { byMarketRepository.save(any()) }

        // 1X2: 1 seleção de 1 total = 1/1 = 1.0000
        // Stake: R$ 15 × 1.0 = R$ 15.00
        // Profit: R$ 30 × 1.0 = R$ 30.00
        assertEquals(BigDecimal("15.00"), capturedEntity.captured.totalStake, "Deve usar stake completo")
        assertEquals(BigDecimal("30.00"), capturedEntity.captured.totalProfit, "Deve usar profit completo")
        assertEquals(BigDecimal("200.00"), capturedEntity.captured.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve ser 200%")
    }

    @Test
    fun `should divide proportionally with 5 selections across 3 markets`() {
        // Given: Ticket com 5 seleções em 3 mercados
        // - 2x Handicap
        // - 2x Total de Gols
        // - 1x Ambas Marcam
        // Stake: R$ 50, Profit: R$ -20 (perda)
        val event = TicketSettledEvent(
            ticketId = 3L,
            userId = 1L,
            providerId = 1L,
            stake = BigDecimal("50.00"),
            totalOdd = BigDecimal("1.50"),
            actualPayout = BigDecimal("30.00"),
            profitLoss = BigDecimal("-20.00"),
            roi = BigDecimal("-40.00"),
            ticketStatus = com.smartbet.domain.enum.TicketStatus.LOST,
            financialStatus = FinancialStatus.PARTIAL_LOSS,
            settledAt = System.currentTimeMillis(),
            selections = listOf(
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = 1L,
                    status = SelectionStatus.WON,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = 2L,
                    status = SelectionStatus.WON,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Total de Gols",
                    tournamentId = 3L,
                    status = SelectionStatus.WON,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Total de Gols",
                    tournamentId = 4L,
                    status = SelectionStatus.LOST,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Ambas Marcam",
                    tournamentId = 5L,
                    status = SelectionStatus.LOST,
                    eventDate = null
                )
            )
        )

        val capturedEntities = mutableListOf<com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity>()
        every { byMarketRepository.findByIdUserIdAndIdMarketType(any(), any()) } returns null
        every { byMarketRepository.save(capture(capturedEntities)) } answers { firstArg() }

        // When
        service.updateOnSettlement(event)

        // Then: Verifica que foram criados 3 registros
        verify(exactly = 3) { byMarketRepository.save(any()) }

        val handicapEntity = capturedEntities.find { it.id.marketType == "Handicap" }
        val totalGolsEntity = capturedEntities.find { it.id.marketType == "Total de Gols" }
        val ambasMarcamEntity = capturedEntities.find { it.id.marketType == "Ambas Marcam" }

        assertNotNull(handicapEntity)
        assertNotNull(totalGolsEntity)
        assertNotNull(ambasMarcamEntity)

        // Handicap: 2/5 = 0.4
        assertEquals(BigDecimal("20.00"), handicapEntity!!.totalStake)
        assertEquals(BigDecimal("-8.00"), handicapEntity.totalProfit)

        // Total de Gols: 2/5 = 0.4
        assertEquals(BigDecimal("20.00"), totalGolsEntity!!.totalStake)
        assertEquals(BigDecimal("-8.00"), totalGolsEntity.totalProfit)

        // Ambas Marcam: 1/5 = 0.2
        assertEquals(BigDecimal("10.00"), ambasMarcamEntity!!.totalStake)
        assertEquals(BigDecimal("-4.00"), ambasMarcamEntity.totalProfit)

        // Valida soma total
        val totalStake = handicapEntity.totalStake
            .add(totalGolsEntity.totalStake)
            .add(ambasMarcamEntity.totalStake)
        assertEquals(BigDecimal("50.00"), totalStake, "Soma deve ser R$ 50")

        val totalProfit = handicapEntity.totalProfit
            .add(totalGolsEntity.totalProfit)
            .add(ambasMarcamEntity.totalProfit)
        assertEquals(BigDecimal("-20.00"), totalProfit, "Soma deve ser R$ -20")
    }

    @Test
    fun `should handle update existing market with proportional division`() {
        // Given: Registro existente + novo ticket do mesmo usuário e mercado
        val existingEntity = com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity(
            id = PerformanceByMarketId(userId = 1L, marketType = "Handicap"),
            totalSelections = 2,
            wins = 2,
            losses = 0,
            voids = 0,
            uniqueTickets = 1,
            ticketsFullWon = 1,
            ticketsPartialWon = 0,
            ticketsBreakEven = 0,
            ticketsPartialLost = 0,
            ticketsTotalLost = 0,
            totalStake = BigDecimal("20.00"),  // Já tinha R$ 20 (2/3 de um ticket de R$ 30)
            totalProfit = BigDecimal("10.00"),
            roi = BigDecimal("50.00"),
            winRate = BigDecimal("100.00"),
            successRate = BigDecimal("100.00"),
            avgOdd = BigDecimal("2.50"),
            firstBetAt = 1000L,
            lastSettledAt = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // Novo ticket: 3 seleções (2 Handicap, 1 Total de Gols), stake R$ 30
        val event = TicketSettledEvent(
            ticketId = 2L,
            userId = 1L,
            providerId = 1L,
            stake = BigDecimal("30.00"),
            totalOdd = BigDecimal("2.00"),
            actualPayout = BigDecimal("45.00"),
            profitLoss = BigDecimal("15.00"),
            roi = BigDecimal("50.00"),
            ticketStatus = com.smartbet.domain.enum.TicketStatus.WIN,
            financialStatus = FinancialStatus.FULL_WIN,
            settledAt = 2000L,
            selections = listOf(
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = 1L,
                    status = SelectionStatus.WON,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = 2L,
                    status = SelectionStatus.WON,
                    eventDate = null
                ),
                TicketSettledEvent.SelectionData(
                    marketType = "Total de Gols",
                    tournamentId = 3L,
                    status = SelectionStatus.WON,
                    eventDate = null
                )
            )
        )

        val capturedEntities = mutableListOf<com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity>()
        every { byMarketRepository.findByIdUserIdAndIdMarketType(1L, "Handicap") } returns existingEntity
        every { byMarketRepository.findByIdUserIdAndIdMarketType(1L, "Total de Gols") } returns null
        every { byMarketRepository.save(capture(capturedEntities)) } answers { firstArg() }

        // When
        service.updateOnSettlement(event)

        // Then: Deve adicionar proporcionalmente (2/3 de R$ 30 = R$ 20)
        val updated = capturedEntities.find { it.id.marketType == "Handicap" }!!
        assertEquals(2, updated.uniqueTickets, "Deve ter 2 tickets únicos agora")

        // Stake anterior (R$ 20) + proporcional novo ticket (R$ 20) = R$ 40
        assertEquals(BigDecimal("40.00"), updated.totalStake, "Deve somar R$ 20 + R$ 20 = R$ 40")

        // Profit anterior (R$ 10) + proporcional novo ticket (R$ 10) = R$ 20
        assertEquals(BigDecimal("20.00"), updated.totalProfit, "Deve somar R$ 10 + R$ 10 = R$ 20")

        // ROI recalculado: (R$ 20 / R$ 40) × 100 = 50%
        assertEquals(BigDecimal("50.00"), updated.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve continuar 50%")
    }
}
