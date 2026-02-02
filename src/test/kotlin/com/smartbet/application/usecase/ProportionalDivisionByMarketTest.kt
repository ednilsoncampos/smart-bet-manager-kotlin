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
 * Testes para validar que cada mercado usa o stake COMPLETO do ticket.
 *
 * A lógica correta é que cada mercado em um ticket múltiplo está exposto
 * ao risco total do bilhete, portanto deve contar o stake completo.
 *
 * Nota: A soma dos stakes por mercado será maior que o stake total investido
 * quando há apostas múltiplas, o que é ESPERADO para análise dimensional.
 */
class PerformanceByMarketFullStakeTest {

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
    fun `should use full stake for each market when ticket has 3 selections in 2 different markets`() {
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

        // Ambos mercados devem ter o stake COMPLETO do ticket
        assertEquals(BigDecimal("30.00"), handicapEntity!!.totalStake, "Handicap deve ter stake completo")
        assertEquals(BigDecimal("15.00"), handicapEntity.totalProfit, "Handicap deve ter profit completo")
        assertEquals(BigDecimal("50.00"), handicapEntity.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve ser 50%")

        assertEquals(BigDecimal("30.00"), totalGolsEntity!!.totalStake, "Total de Gols deve ter stake completo")
        assertEquals(BigDecimal("15.00"), totalGolsEntity.totalProfit, "Total de Gols deve ter profit completo")
        assertEquals(BigDecimal("50.00"), totalGolsEntity.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve ser 50%")

        // Valida contadores de seleções (baseado em seleções individuais por mercado)
        assertEquals(2, handicapEntity.totalSelections, "Handicap deve ter 2 seleções")
        assertEquals(1, totalGolsEntity.totalSelections, "Total de Gols deve ter 1 seleção")

        // NOTA: Soma dos stakes (R$ 30 + R$ 30 = R$ 60) > stake investido (R$ 30)
        // Isso é ESPERADO e CORRETO para análise por dimensão
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

        assertEquals(BigDecimal("15.00"), capturedEntity.captured.totalStake, "Deve usar stake completo")
        assertEquals(BigDecimal("30.00"), capturedEntity.captured.totalProfit, "Deve usar profit completo")
        assertEquals(BigDecimal("200.00"), capturedEntity.captured.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve ser 200%")
    }

    @Test
    fun `should use full stake with 5 selections across 3 markets`() {
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

        // Todos mercados devem ter stake e profit COMPLETOS
        assertEquals(BigDecimal("50.00"), handicapEntity!!.totalStake)
        assertEquals(BigDecimal("-20.00"), handicapEntity.totalProfit)
        assertEquals(BigDecimal("-40.00"), handicapEntity.roi.setScale(2, RoundingMode.HALF_UP))

        assertEquals(BigDecimal("50.00"), totalGolsEntity!!.totalStake)
        assertEquals(BigDecimal("-20.00"), totalGolsEntity.totalProfit)
        assertEquals(BigDecimal("-40.00"), totalGolsEntity.roi.setScale(2, RoundingMode.HALF_UP))

        assertEquals(BigDecimal("50.00"), ambasMarcamEntity!!.totalStake)
        assertEquals(BigDecimal("-20.00"), ambasMarcamEntity.totalProfit)
        assertEquals(BigDecimal("-40.00"), ambasMarcamEntity.roi.setScale(2, RoundingMode.HALF_UP))

        // Valida contadores de seleções por mercado
        assertEquals(2, handicapEntity.wins, "Handicap: 2 seleções ganhas")
        assertEquals(0, handicapEntity.losses, "Handicap: 0 seleções perdidas")

        assertEquals(1, totalGolsEntity.wins, "Total de Gols: 1 seleção ganha")
        assertEquals(1, totalGolsEntity.losses, "Total de Gols: 1 seleção perdida")

        assertEquals(0, ambasMarcamEntity.wins, "Ambas Marcam: 0 seleções ganhas")
        assertEquals(1, ambasMarcamEntity.losses, "Ambas Marcam: 1 seleção perdida")

        // NOTA: Soma dos stakes = R$ 150 (3 × R$ 50) > stake investido (R$ 50)
        // Isso é ESPERADO para análise dimensional
    }

    @Test
    fun `should handle update existing market with full stake`() {
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
            totalStake = BigDecimal("30.00"),  // Já tinha R$ 30 de um ticket anterior
            totalProfit = BigDecimal("15.00"),
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

        // Then: Deve adicionar stake COMPLETO (R$ 30)
        val updated = capturedEntities.find { it.id.marketType == "Handicap" }!!
        assertEquals(2, updated.uniqueTickets, "Deve ter 2 tickets únicos agora")

        // Stake anterior (R$ 30) + novo ticket completo (R$ 30) = R$ 60
        assertEquals(BigDecimal("60.00"), updated.totalStake, "Deve somar R$ 30 + R$ 30 = R$ 60")

        // Profit anterior (R$ 15) + novo ticket completo (R$ 15) = R$ 30
        assertEquals(BigDecimal("30.00"), updated.totalProfit, "Deve somar R$ 15 + R$ 15 = R$ 30")

        // ROI recalculado: (R$ 30 / R$ 60) × 100 = 50%
        assertEquals(BigDecimal("50.00"), updated.roi.setScale(2, RoundingMode.HALF_UP), "ROI deve continuar 50%")
    }

    @Test
    fun `should calculate winRate based on selections and successRate based on tickets`() {
        // Given: Ticket com 10 seleções (7 wins, 3 losses) e status PARTIAL_WIN
        val event = TicketSettledEvent(
            ticketId = 4L,
            userId = 1L,
            providerId = 1L,
            stake = BigDecimal("100.00"),
            totalOdd = BigDecimal("5.00"),
            actualPayout = BigDecimal("120.00"),
            profitLoss = BigDecimal("20.00"),
            roi = BigDecimal("20.00"),
            ticketStatus = com.smartbet.domain.enum.TicketStatus.WIN,
            financialStatus = FinancialStatus.PARTIAL_WIN,
            settledAt = System.currentTimeMillis(),
            selections = (1..7).map {
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = it.toLong(),
                    status = SelectionStatus.WON,
                    eventDate = null
                )
            } + (8..10).map {
                TicketSettledEvent.SelectionData(
                    marketType = "Handicap",
                    tournamentId = it.toLong(),
                    status = SelectionStatus.LOST,
                    eventDate = null
                )
            }
        )

        val capturedEntity = slot<com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity>()
        every { byMarketRepository.findByIdUserIdAndIdMarketType(any(), any()) } returns null
        every { byMarketRepository.save(capture(capturedEntity)) } answers { firstArg() }

        // When
        service.updateOnSettlement(event)

        // Then
        val entity = capturedEntity.captured

        // winRate = (7 wins / 10 seleções) × 100 = 70%
        assertEquals(BigDecimal("70.00"), entity.winRate.setScale(2, RoundingMode.HALF_UP), "winRate deve ser 70%")

        // successRate = (1 partialWin / 1 ticket) × 100 = 100%
        assertEquals(BigDecimal("100.00"), entity.successRate.setScale(2, RoundingMode.HALF_UP), "successRate deve ser 100%")

        assertEquals(7, entity.wins, "Deve ter 7 seleções ganhas")
        assertEquals(3, entity.losses, "Deve ter 3 seleções perdidas")
        assertEquals(1, entity.ticketsPartialWon, "Deve ter 1 ticket com vitória parcial")
    }
}
