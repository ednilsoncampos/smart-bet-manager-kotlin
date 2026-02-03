package com.smartbet.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartbet.application.dto.ImportTicketRequest
import com.smartbet.application.usecase.TicketService
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.infrastructure.persistence.repository.BetSelectionComponentRepository
import com.smartbet.infrastructure.provider.gateway.HttpGateway
import com.smartbet.presentation.controller.AnalyticsController
import io.mockk.every
import io.mockk.mockk
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.math.BigDecimal

/**
 * Teste de integração para o endpoint de performance por mercado.
 *
 * Este teste valida o comportamento do parâmetro expandBetBuilder=true,
 * que deve extrair componentes de "Criar Aposta" (Bet Builder) e agregá-los
 * com mercados normais.
 *
 * Cenário de teste:
 * - 4 bilhetes da Superbet (system bets)
 * - 22 seleções de "Dupla Chance" (18 componentes únicos)
 * - Todas com status WIN
 * - Espera-se winRate = 100% quando expandBetBuilder=true
 */
@Ignore
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
//@DisplayName("Performance por Mercado - Integration Test")
class PerformanceByMarketIntegrationTest {

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("smart_bet_test")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.flyway.enabled") { "true" }
        }
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockHttpGateway(): HttpGateway = mockk(relaxed = true)
    }

    @Autowired
    private lateinit var ticketService: TicketService

    @Autowired
    private lateinit var analyticsController: AnalyticsController

    @Autowired
    private lateinit var httpGateway: HttpGateway

    @Autowired
    private lateinit var selectionComponentRepository: BetSelectionComponentRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val userId = 1L

    @BeforeEach
    fun setup() {
        // Mocka as respostas da API da Superbet para os 4 bilhetes
        setupMockResponses()
    }

    @Ignore
    @Transactional
    @DisplayName("deve retornar Dupla Chance com 100% winRate quando expandBetBuilder=true")
    fun `should return Dupla Chance with 100 percent winRate when expandBetBuilder is true`() {
        // Given: 4 bilhetes importados com seleções de Dupla Chance
        val ticketUrls = listOf(
            "https://superbet.bet.br/bilhete-compartilhado/890I-QDH292",
            "https://superbet.bet.br/bilhete-compartilhado/891V-YHIDVV",
            "https://superbet.bet.br/bilhete-compartilhado/898L-7Y22F9",
            "https://superbet.bet.br/bilhete-compartilhado/8909-QHO50Q"
        )

        // When: Importa os 4 bilhetes
        ticketUrls.forEach { url ->
            val request = ImportTicketRequest(url = url, bankrollId = null)
            ticketService.importFromUrl(userId, request)
        }

        // Valida que os componentes foram salvos corretamente
        val allComponents = selectionComponentRepository.findByUserId(userId)
        val duplaChanceComponents = allComponents.filter { it.marketName == "Dupla Chance" }

        println("Total de componentes salvos: ${allComponents.size}")
        println("Componentes de Dupla Chance: ${duplaChanceComponents.size}")
        println("Status dos componentes de Dupla Chance:")
        duplaChanceComponents.groupBy { it.status }.forEach { (status, components) ->
            println("  $status: ${components.size}")
        }

        // Valida que todos os componentes de Dupla Chance têm status WIN
        val allDuplaChanceWon = duplaChanceComponents.all { it.status == SelectionStatus.WON }
        assertTrue(allDuplaChanceWon, "Todos os componentes de Dupla Chance devem ter status WON")

        // Then: Chama o endpoint com expandBetBuilder=true
        val response = analyticsController.getPerformanceByMarket(userId, expandBetBuilder = true)

        assertNotNull(response)
        assertEquals(200, response.statusCode.value())

        val markets = response.body ?: emptyList()
        println("\nMercados retornados:")
        markets.forEach { market ->
            println("  ${market.marketType}: ${market.totalSelections} seleções, winRate=${market.winRate}%")
        }

        // Valida que "Dupla Chance" existe no resultado
        val duplaChanceMarket = markets.find { it.marketType == "Dupla Chance" }
        assertNotNull(duplaChanceMarket, "Mercado 'Dupla Chance' deveria existir no resultado")

        // Valida as métricas
        duplaChanceMarket?.let { market ->
            println("\nDetalhes do mercado 'Dupla Chance':")
            println("  Total de seleções: ${market.totalSelections}")
            println("  Wins: ${market.wins}")
            println("  Losses: ${market.losses}")
            println("  WinRate: ${market.winRate}%")

            // Valida que todas as seleções são vitórias
            assertEquals(market.totalSelections, market.wins,
                "Todas as ${market.totalSelections} seleções devem ser vitórias")

            // Valida winRate = 100%
            assertEquals(BigDecimal("100.00"), market.winRate,
                "WinRate deve ser 100% pois todas as seleções de Dupla Chance foram WIN")

            // Valida que não há losses
            assertEquals(0L, market.losses, "Não deve haver losses")

            // Valida que betBuilderComponents é null quando expandBetBuilder=true
            assertNull(market.betBuilderComponents,
                "betBuilderComponents deve ser null quando expandBetBuilder=true")
        }
    }

    @Ignore
    @Transactional
    @DisplayName("deve retornar Criar Aposta com betBuilderComponents quando expandBetBuilder=false")
    fun `should return Criar Aposta with betBuilderComponents when expandBetBuilder is false`() {
        // Given: 4 bilhetes importados
        val ticketUrls = listOf(
            "https://superbet.bet.br/bilhete-compartilhado/890I-QDH292",
            "https://superbet.bet.br/bilhete-compartilhado/891V-YHIDVV",
            "https://superbet.bet.br/bilhete-compartilhado/898L-7Y22F9",
            "https://superbet.bet.br/bilhete-compartilhado/8909-QHO50Q"
        )

        ticketUrls.forEach { url ->
            val request = ImportTicketRequest(url = url, bankrollId = null)
            ticketService.importFromUrl(userId, request)
        }

        // When: Chama o endpoint com expandBetBuilder=false (padrão)
        val response = analyticsController.getPerformanceByMarket(userId, expandBetBuilder = false)

        assertNotNull(response)
        assertEquals(200, response.statusCode.value())

        val markets = response.body ?: emptyList()

        // Then: Valida que "Criar Aposta" existe e contém betBuilderComponents
        val criarApostaMarket = markets.find { it.marketType == "Criar Aposta" }
        assertNotNull(criarApostaMarket, "Mercado 'Criar Aposta' deveria existir no resultado")

        criarApostaMarket?.let { market ->
            assertNotNull(market.betBuilderComponents,
                "betBuilderComponents não deve ser null quando expandBetBuilder=false")

            val components = market.betBuilderComponents ?: emptyList()
            val duplaChanceComponents = components.filter { it.marketName == "Dupla Chance" }

            println("\nComponentes de Dupla Chance no Bet Builder:")
            duplaChanceComponents.forEach { component ->
                println("  ${component.eventName} - ${component.selectionName}: " +
                        "${component.wins}/${component.totalBets} (${component.winRate}%)")
            }

            assertTrue(duplaChanceComponents.isNotEmpty(),
                "Deve haver componentes de Dupla Chance dentro de 'Criar Aposta'")
        }
    }

    private fun setupMockResponses() {
        // Carrega os JSONs de fixtures
        val json890I = loadFixture("890I-QDH292.json")
        val json891V = loadFixture("891V-YHIDVV.json")
        val json898L = loadFixture("898L-7Y22F9.json")
        val json8909 = loadFixture("8909-QHO50Q.json")

        // Mocka as respostas do HttpGateway
        every {
            httpGateway.get("https://superbet.bet.br/api/content/betslip/890I-QDH292")
        } returns json890I

        every {
            httpGateway.get("https://superbet.bet.br/api/content/betslip/891V-YHIDVV")
        } returns json891V

        every {
            httpGateway.get("https://superbet.bet.br/api/content/betslip/898L-7Y22F9")
        } returns json898L

        every {
            httpGateway.get("https://superbet.bet.br/api/content/betslip/8909-QHO50Q")
        } returns json8909
    }

    private fun loadFixture(filename: String): String {
        val resourcePath = "/claude-analise/$filename"
        return this::class.java.getResource(resourcePath)?.readText()
            ?: throw IllegalArgumentException("Fixture not found: $resourcePath")
    }
}
