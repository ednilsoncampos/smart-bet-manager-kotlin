package com.smartbet.application.usecase

import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.infrastructure.persistence.entity.*
import com.smartbet.infrastructure.persistence.repository.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Teste unitário para validar a funcionalidade expandBetBuilder do PerformanceAnalyticService.
 *
 * Este teste reproduz o cenário dos 4 bilhetes importados com seleções de "Dupla Chance".
 *
 * Cenário:
 * - 4 bilhetes (system bets) importados
 * - 22 seleções de "Dupla Chance" em "Criar Aposta"
 * - Todas com status WIN
 * - Espera-se winRate = 100% quando expandBetBuilder=true
 */
@DisplayName("PerformanceAnalyticService - expandBetBuilder")
class PerformanceByMarketExpandBetBuilderTest {

    private lateinit var service: PerformanceAnalyticService
    private lateinit var byMarketRepository: PerformanceByMarketRepository
    private lateinit var selectionComponentRepository: BetSelectionComponentRepository
    private lateinit var overallRepository: PerformanceOverallRepository
    private lateinit var byProviderRepository: PerformanceByProviderRepository
    private lateinit var byMonthRepository: PerformanceByMonthRepository
    private lateinit var byTournamentRepository: PerformanceByTournamentRepository
    private lateinit var providerRepository: BettingProviderRepository
    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var ticketRepository: BetTicketRepository

    private val userId = 1L

    @BeforeEach
    fun setup() {
        overallRepository = mockk(relaxed = true)
        byProviderRepository = mockk(relaxed = true)
        byMarketRepository = mockk(relaxed = true)
        byMonthRepository = mockk(relaxed = true)
        byTournamentRepository = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        tournamentRepository = mockk(relaxed = true)
        selectionComponentRepository = mockk(relaxed = true)
        ticketRepository = mockk(relaxed = true)

        service = PerformanceAnalyticService(
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

    @Test
    @DisplayName("deve agregar Dupla Chance com winRate 100% quando expandBetBuilder=true")
    fun `should aggregate Dupla Chance with 100 percent winRate when expandBetBuilder is true`() {
        // Given: Dados agregados na tabela analytics.performance_by_market
        val criarApostaPerformance = PerformanceByMarketEntity(
            id = PerformanceByMarketId(userId, "Criar Aposta"),
            totalSelections = 84, // Total de componentes em "Criar Aposta"
            wins = 82,
            losses = 2,
            voids = 0,
            uniqueTickets = 4,
            ticketsFullWon = 1,
            ticketsPartialWon = 1,
            ticketsBreakEven = 0,
            ticketsPartialLost = 1,
            ticketsTotalLost = 1,
            winRate = BigDecimal("97.62"),
            successRate = BigDecimal("50.00"),
            avgOdd = BigDecimal("99.3677"),
            firstBetAt = 1768147156350L,
            lastSettledAt = 1769291661953L
        )

        // Componentes de Dupla Chance dentro do Bet Builder
        val duplaChanceComponents = createDuplaChanceComponents()

        every { byMarketRepository.findByIdUserId(userId) } returns listOf(criarApostaPerformance)
        every { selectionComponentRepository.findByUserId(userId) } returns duplaChanceComponents

        // When: Chama o service com expandBetBuilder=true
        val result = service.getPerformanceByMarket(userId, expandBetBuilder = true)

        // Then: Valida que "Dupla Chance" foi extraído e agregado
        val duplaChanceMarket = result.find { it.marketType == "Dupla Chance" }
        assertNotNull(duplaChanceMarket, "Mercado 'Dupla Chance' deve existir quando expandBetBuilder=true")

        duplaChanceMarket?.let { market ->
            println("Mercado 'Dupla Chance':")
            println("  Total Seleções: ${market.totalSelections}")
            println("  Wins: ${market.wins}")
            println("  Losses: ${market.losses}")
            println("  WinRate: ${market.winRate}%")
            println("  Unique Tickets: ${market.uniqueTickets}")

            // Valida que contém 18 seleções únicas de Dupla Chance
            assertEquals(18L, market.totalSelections,
                "Deve ter 18 seleções únicas de Dupla Chance")

            // Valida que todas são vitórias
            assertEquals(18L, market.wins,
                "Todas as 18 seleções devem ser vitórias")

            // Valida que não há derrotas
            assertEquals(0L, market.losses,
                "Não deve haver derrotas")

            // Valida winRate = 100%
            assertEquals(BigDecimal("100.00"), market.winRate,
                "WinRate deve ser 100% pois todas as seleções de Dupla Chance foram WIN")

            // Valida que betBuilderComponents é null quando expandBetBuilder=true
            assertNull(market.betBuilderComponents,
                "betBuilderComponents deve ser null quando mercado foi expandido")

            // Valida que uniqueTickets foi agregado (4 tickets)
            assertEquals(4L, market.uniqueTickets,
                "Deve agregar os 4 tickets únicos do Bet Builder")
        }

        // Valida que "Criar Aposta" foi removido da lista
        val criarApostaMarket = result.find { it.marketType == "Criar Aposta" }
        assertNull(criarApostaMarket,
            "'Criar Aposta' não deve existir quando expandBetBuilder=true")
    }

    @Test
    @DisplayName("deve manter Criar Aposta com components quando expandBetBuilder=false")
    fun `should keep Criar Aposta with components when expandBetBuilder is false`() {
        // Given: Mesmos dados
        val criarApostaPerformance = PerformanceByMarketEntity(
            id = PerformanceByMarketId(userId, "Criar Aposta"),
            totalSelections = 84,
            wins = 82,
            losses = 2,
            voids = 0,
            uniqueTickets = 4,
            ticketsFullWon = 1,
            ticketsPartialWon = 1,
            ticketsBreakEven = 0,
            ticketsPartialLost = 1,
            ticketsTotalLost = 1,
            winRate = BigDecimal("97.62"),
            successRate = BigDecimal("50.00"),
            avgOdd = BigDecimal("99.3677"),
            firstBetAt = 1768147156350L,
            lastSettledAt = 1769291661953L
        )

        val duplaChanceComponents = createDuplaChanceComponents()

        every { byMarketRepository.findByIdUserId(userId) } returns listOf(criarApostaPerformance)
        every { selectionComponentRepository.findByUserId(userId) } returns duplaChanceComponents

        // When: Chama o service com expandBetBuilder=false (padrão)
        val result = service.getPerformanceByMarket(userId, expandBetBuilder = false)

        // Then: Valida que "Criar Aposta" existe e contém betBuilderComponents
        val criarApostaMarket = result.find { it.marketType == "Criar Aposta" }
        assertNotNull(criarApostaMarket, "'Criar Aposta' deve existir quando expandBetBuilder=false")

        criarApostaMarket?.let { market ->
            assertNotNull(market.betBuilderComponents,
                "betBuilderComponents não deve ser null quando expandBetBuilder=false")

            val components = market.betBuilderComponents ?: emptyList()
            val duplaChanceComps = components.filter { it.marketName == "Dupla Chance" }

            println("Componentes de Dupla Chance dentro de 'Criar Aposta':")
            duplaChanceComps.forEach { comp ->
                println("  ${comp.eventName} - ${comp.selectionName}: " +
                        "${comp.wins}/${comp.totalBets} (${comp.winRate}%)")
            }

            // Valida que há componentes de Dupla Chance
            assertTrue(duplaChanceComps.isNotEmpty(),
                "Deve haver componentes de Dupla Chance")

            // Valida que todos os componentes têm 100% win rate
            duplaChanceComps.forEach { comp ->
                assertEquals(BigDecimal("100.0000"), comp.winRate,
                    "Componente ${comp.eventName} - ${comp.selectionName} deve ter 100% winRate")
            }
        }

        // Valida que "Dupla Chance" NÃO existe como mercado separado
        val duplaChanceMarket = result.find { it.marketType == "Dupla Chance" }
        assertNull(duplaChanceMarket,
            "'Dupla Chance' não deve existir como mercado separado quando expandBetBuilder=false")
    }

    /**
     * Cria 18 componentes únicos de Dupla Chance (todos com status WIN) simulando os 4 bilhetes.
     * Não inclui duplicatas para manter a simplicidade do teste.
     */
    private fun createDuplaChanceComponents(): List<BetSelectionComponentEntity> {
        val components = mutableListOf<BetSelectionComponentEntity>()

        // 18 eventos únicos de Dupla Chance todos com status WIN
        val uniqueEvents = listOf(
            Triple("Sochaux x Lens", "X2", 1L),
            Triple("Corinthians x Ponte Preta", "1X", 2L),
            Triple("Bahia x Jequié", "1X", 3L),
            Triple("Panathinaikos x Panserraikos", "1X", 4L),
            Triple("Dundee FC x Hearts", "X2", 5L),
            Triple("Aberdeen x Rangers", "X2", 6L),
            Triple("CSA AL x Coruripe AL", "1X", 7L),
            Triple("Capital DF x Samambaia", "1X", 8L),
            Triple("PSV Eindhoven x NAC Breda", "1 ou Empate", 9L),
            Triple("Arouca x Sporting Lisboa", "Empate ou 2", 10L),
            Triple("Karagumruk x Galatasaray", "Empate ou 2", 11L),
            Triple("Club Brugge x Zulte Waregem", "1 ou Empate", 12L),
            Triple("Athletico PR x Galo Maringá", "1 ou Empate", 13L),
            Triple("Operário PR x Foz do Iguaçu", "1 ou Empate", 14L),
            Triple("Asteras Tripolis x AEK Atenas", "Empate ou 2", 15L),
            Triple("Verona x Lazio", "X2", 16L),
            Triple("Barcelona x Real Madrid", "1X", 17L),
            Triple("Lille x Olympique Lyon", "X2", 18L)
        )

        uniqueEvents.forEach { (event, selection, idx) ->
            components.add(createComponent(idx, event, "Dupla Chance", selection))
        }

        return components
    }

    private fun createComponent(
        id: Long,
        eventName: String,
        marketName: String,
        selectionName: String
    ): BetSelectionComponentEntity {
        // Cria mock BetSelectionEntity com eventName para permitir agrupamento correto
        val mockSelection = mockk<BetSelectionEntity>(relaxed = true)
        every { mockSelection.eventName } returns eventName
        every { mockSelection.id } returns id

        return BetSelectionComponentEntity(
            id = id,
            selection = mockSelection,
            marketId = null,
            marketName = marketName,
            selectionName = selectionName,
            status = SelectionStatus.WON
        )
    }
}
