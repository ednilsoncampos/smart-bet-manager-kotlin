package com.smartbet.infrastructure.provider.strategy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.smartbet.infrastructure.provider.gateway.HttpGateway
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BettingProviderFactory")
class BettingProviderFactoryTest {

    private lateinit var factory: BettingProviderFactory
    private lateinit var superbetStrategy: SuperbetStrategy
    private lateinit var betanoStrategy: BetanoStrategy
    private lateinit var httpGateway: HttpGateway

    @BeforeEach
    fun setup() {
        val objectMapper = jacksonObjectMapper()
        httpGateway = mockk(relaxed = true)

        superbetStrategy = SuperbetStrategy(objectMapper, httpGateway)
        betanoStrategy = BetanoStrategy(objectMapper)
        factory = BettingProviderFactory(listOf(superbetStrategy, betanoStrategy))
    }
    
    @Nested
    @DisplayName("findStrategyForUrl()")
    inner class FindStrategyForUrlTests {
        
        @Test
        @DisplayName("deve retornar SuperbetStrategy para URL da Superbet")
        fun shouldReturnSuperbetStrategyForSuperbetUrl() {
            val url = "https://superbet.bet.br/bilhete-compartilhado/ABC123"
            val strategy = factory.findStrategyForUrl(url)
            
            assertNotNull(strategy)
            assertEquals("superbet", strategy?.slug)
        }
        
        @Test
        @DisplayName("deve retornar BetanoStrategy para URL da Betano")
        fun shouldReturnBetanoStrategyForBetanoUrl() {
            val url = "https://www.betano.bet.br/mybets/123456789"
            val strategy = factory.findStrategyForUrl(url)
            
            assertNotNull(strategy)
            assertEquals("betano", strategy?.slug)
        }
        
        @Test
        @DisplayName("deve retornar null para URL não suportada")
        fun shouldReturnNullForUnsupportedUrl() {
            val url = "https://bet365.com/bet/123"
            val strategy = factory.findStrategyForUrl(url)
            
            assertNull(strategy)
        }
    }
    
    @Nested
    @DisplayName("getStrategy()")
    inner class GetStrategyTests {
        
        @Test
        @DisplayName("deve retornar strategy pelo slug")
        fun shouldReturnStrategyBySlug() {
            val strategy = factory.getStrategy("superbet")
            
            assertNotNull(strategy)
            assertEquals("Superbet", strategy?.name)
        }
        
        @Test
        @DisplayName("deve retornar null para slug não registrado")
        fun shouldReturnNullForUnregisteredSlug() {
            val strategy = factory.getStrategy("bet365")
            
            assertNull(strategy)
        }
    }
    
    @Nested
    @DisplayName("isSupported()")
    inner class IsSupportedTests {
        
        @Test
        @DisplayName("deve retornar true para URL suportada")
        fun shouldReturnTrueForSupportedUrl() {
            assertTrue(factory.isSupported("https://superbet.bet.br/bilhete-compartilhado/ABC"))
            assertTrue(factory.isSupported("https://betano.bet.br/mybets/123"))
        }
        
        @Test
        @DisplayName("deve retornar false para URL não suportada")
        fun shouldReturnFalseForUnsupportedUrl() {
            assertFalse(factory.isSupported("https://bet365.com/bet/123"))
            assertFalse(factory.isSupported("https://google.com"))
        }
    }
    
    @Nested
    @DisplayName("extractProviderNameFromUrl()")
    inner class ExtractProviderNameFromUrlTests {
        
        @Test
        @DisplayName("deve extrair nome de provedores conhecidos")
        fun shouldExtractKnownProviderNames() {
            assertEquals("Superbet", factory.extractProviderNameFromUrl("https://superbet.bet.br/test"))
            assertEquals("Betano", factory.extractProviderNameFromUrl("https://www.betano.bet.br/test"))
            assertEquals("Bet365", factory.extractProviderNameFromUrl("https://bet365.com/bet"))
            assertEquals("Betfair", factory.extractProviderNameFromUrl("https://betfair.com/exchange"))
            assertEquals("Pixbet", factory.extractProviderNameFromUrl("https://pixbet.com/sports"))
        }
        
        @Test
        @DisplayName("deve retornar domínio para provedores desconhecidos")
        fun shouldReturnDomainForUnknownProviders() {
            val result = factory.extractProviderNameFromUrl("https://www.unknownbet.com/test")
            assertEquals("unknownbet", result)
        }
        
        @Test
        @DisplayName("deve retornar null para URL inválida")
        fun shouldReturnNullForInvalidUrl() {
            val result = factory.extractProviderNameFromUrl("not-a-url")
            assertNull(result)
        }
    }
    
    @Nested
    @DisplayName("getRegisteredProviders()")
    inner class GetRegisteredProvidersTests {
        
        @Test
        @DisplayName("deve retornar lista de slugs registrados")
        fun shouldReturnListOfRegisteredSlugs() {
            val providers = factory.getRegisteredProviders()
            
            assertEquals(2, providers.size)
            assertTrue(providers.contains("superbet"))
            assertTrue(providers.contains("betano"))
        }
    }
    
    @Nested
    @DisplayName("getAllStrategies()")
    inner class GetAllStrategiesTests {
        
        @Test
        @DisplayName("deve retornar todas as strategies registradas")
        fun shouldReturnAllRegisteredStrategies() {
            val strategies = factory.getAllStrategies()
            
            assertEquals(2, strategies.size)
        }
    }
}
