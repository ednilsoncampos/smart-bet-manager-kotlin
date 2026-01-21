package com.smartbet.infrastructure.provider.strategy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BetanoStrategy")
class BetanoStrategyTest {
    
    private lateinit var strategy: BetanoStrategy
    private lateinit var objectMapper: ObjectMapper
    
    @BeforeEach
    fun setup() {
        objectMapper = jacksonObjectMapper()
        strategy = BetanoStrategy(objectMapper)
    }
    
    @Nested
    @DisplayName("canHandle()")
    inner class CanHandleTests {
        
        @Test
        @DisplayName("deve retornar true para URL válida da Betano")
        fun shouldReturnTrueForValidBetanoUrl() {
            val url = "https://www.betano.bet.br/mybets/123456789"
            assertTrue(strategy.canHandle(url))
        }
        
        @Test
        @DisplayName("deve retornar true para URL sem www")
        fun shouldReturnTrueForUrlWithoutWww() {
            val url = "https://betano.bet.br/mybets/987654321"
            assertTrue(strategy.canHandle(url))
        }
        
        @Test
        @DisplayName("deve retornar true para URL com domínio .com.br")
        fun shouldReturnTrueForComBrDomain() {
            val url = "https://betano.com.br/mybets/111222333"
            assertTrue(strategy.canHandle(url))
        }
        
        @Test
        @DisplayName("deve retornar false para URL de outra casa")
        fun shouldReturnFalseForOtherProvider() {
            val url = "https://superbet.bet.br/bilhete-compartilhado/ABC123"
            assertFalse(strategy.canHandle(url))
        }
        
        @Test
        @DisplayName("deve retornar false para URL inválida")
        fun shouldReturnFalseForInvalidUrl() {
            val url = "https://google.com"
            assertFalse(strategy.canHandle(url))
        }
    }
    
    @Nested
    @DisplayName("extractTicketCode()")
    inner class ExtractTicketCodeTests {
        
        @Test
        @DisplayName("deve extrair código numérico do bilhete da URL")
        fun shouldExtractNumericTicketCodeFromUrl() {
            val url = "https://www.betano.bet.br/mybets/123456789"
            val code = strategy.extractTicketCode(url)
            assertEquals("123456789", code)
        }
        
        @Test
        @DisplayName("deve retornar null para URL sem código")
        fun shouldReturnNullForUrlWithoutCode() {
            val url = "https://betano.bet.br/sports"
            val code = strategy.extractTicketCode(url)
            assertNull(code)
        }
    }
    
    @Nested
    @DisplayName("buildApiUrl()")
    inner class BuildApiUrlTests {
        
        @Test
        @DisplayName("deve construir URL da API com template padrão")
        fun shouldBuildApiUrlWithDefaultTemplate() {
            val code = "123456789"
            val apiUrl = strategy.buildApiUrl(code)
            assertEquals("https://www.betano.bet.br/api/betslip/v3/getBetslipById/123456789", apiUrl)
        }
    }
    
    @Nested
    @DisplayName("parseResponse()")
    inner class ParseResponseTests {
        
        @Test
        @DisplayName("deve lançar UnsupportedOperationException indicando que parser está em desenvolvimento")
        fun shouldThrowUnsupportedOperationException() {
            val json = """
            {
                "data": {
                    "betslipId": "BET-001",
                    "stake": 100.00,
                    "totalOdds": 1.80,
                    "status": "won",
                    "legs": []
                }
            }
            """.trimIndent()
            
            val exception = assertThrows(UnsupportedOperationException::class.java) {
                strategy.parseResponse(json)
            }
            
            assertTrue(exception.message?.contains("Parser da Betano em desenvolvimento") == true)
            assertTrue(exception.message?.contains("API da Betano não disponibiliza endpoint público") == true)
        }
        
        @Test
        @DisplayName("deve indicar para usar outra casa de apostas na mensagem de erro")
        fun shouldSuggestUsingOtherProvider() {
            val json = "{}"
            
            val exception = assertThrows(UnsupportedOperationException::class.java) {
                strategy.parseResponse(json)
            }
            
            assertTrue(exception.message?.contains("utilize outra casa de apostas") == true)
        }
    }
}
