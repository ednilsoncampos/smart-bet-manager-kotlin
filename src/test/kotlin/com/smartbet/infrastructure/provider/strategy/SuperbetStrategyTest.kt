package com.smartbet.infrastructure.provider.strategy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.enum.TicketStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("SuperbetStrategy")
class SuperbetStrategyTest {
    
    private lateinit var strategy: SuperbetStrategy
    private lateinit var objectMapper: ObjectMapper
    
    @BeforeEach
    fun setup() {
        objectMapper = jacksonObjectMapper()
        strategy = SuperbetStrategy(objectMapper)
    }
    
    @Nested
    @DisplayName("canHandle()")
    inner class CanHandleTests {
        
        @Test
        @DisplayName("deve retornar true para URL válida da Superbet")
        fun shouldReturnTrueForValidSuperbetUrl() {
            val url = "https://superbet.bet.br/bilhete-compartilhado/ABC123"
            assertTrue(strategy.canHandle(url))
        }
        
        @Test
        @DisplayName("deve retornar true para URL com domínio .com.br")
        fun shouldReturnTrueForComBrDomain() {
            val url = "https://superbet.com.br/bilhete-compartilhado/XYZ789"
            assertTrue(strategy.canHandle(url))
        }
        
        @Test
        @DisplayName("deve retornar false para URL de outra casa")
        fun shouldReturnFalseForOtherProvider() {
            val url = "https://betano.bet.br/mybets/123456"
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
        @DisplayName("deve extrair código do bilhete da URL")
        fun shouldExtractTicketCodeFromUrl() {
            val url = "https://superbet.bet.br/bilhete-compartilhado/ABC-123-XYZ"
            val code = strategy.extractTicketCode(url)
            assertEquals("ABC-123-XYZ", code)
        }
        
        @Test
        @DisplayName("deve retornar null para URL sem código")
        fun shouldReturnNullForUrlWithoutCode() {
            val url = "https://superbet.bet.br/home"
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
            val code = "ABC123"
            val apiUrl = strategy.buildApiUrl(code)
            assertEquals("https://superbet.bet.br/api/content/betslip/ABC123", apiUrl)
        }
        
        @Test
        @DisplayName("deve construir URL da API com template customizado")
        fun shouldBuildApiUrlWithCustomTemplate() {
            val code = "ABC123"
            val customTemplate = "https://api.superbet.com/v2/betslip/{CODE}"
            val apiUrl = strategy.buildApiUrl(code, customTemplate)
            assertEquals("https://api.superbet.com/v2/betslip/ABC123", apiUrl)
        }
    }
    
    @Nested
    @DisplayName("parseResponse()")
    inner class ParseResponseTests {
        
        @Test
        @DisplayName("deve parsear resposta de bilhete simples ganho")
        fun shouldParseWonSingleTicket() {
            val json = """
            {
                "data": {
                    "ticketId": "TICKET-001",
                    "stake": 100.00,
                    "totalOdds": 2.50,
                    "potentialWin": 250.00,
                    "payout": 250.00,
                    "status": "won",
                    "placedAt": "2024-01-15T10:30:00Z",
                    "settledAt": "2024-01-15T12:00:00Z",
                    "selections": [
                        {
                            "id": "SEL-001",
                            "eventName": "Flamengo x Palmeiras",
                            "tournamentName": "Brasileirão",
                            "marketName": "Resultado Final",
                            "outcomeName": "Flamengo",
                            "odds": 2.50,
                            "status": "won",
                            "eventDate": "2024-01-15T16:00:00Z",
                            "result": "2-1"
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals("TICKET-001", result.externalTicketId)
            assertEquals(BetType.SINGLE, result.betType)
            assertEquals(BigDecimal("100.0"), result.stake)
            assertEquals(BigDecimal("2.5"), result.totalOdd)
            assertEquals(BigDecimal("250.0"), result.potentialPayout)
            assertEquals(BigDecimal("250.0"), result.actualPayout)
            assertEquals(TicketStatus.WON, result.ticketStatus)
            assertEquals(1, result.selections.size)
            
            val selection = result.selections[0]
            assertEquals("Flamengo x Palmeiras", selection.eventName)
            assertEquals("Brasileirão", selection.tournamentName)
            assertEquals("Resultado Final", selection.marketType)
            assertEquals("Flamengo", selection.selection)
            assertEquals(BigDecimal("2.5"), selection.odd)
            assertEquals(SelectionStatus.WON, selection.status)
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete múltiplo perdido")
        fun shouldParseLostMultipleTicket() {
            val json = """
            {
                "data": {
                    "ticketId": "TICKET-002",
                    "stake": 50.00,
                    "totalOdds": 5.00,
                    "potentialWin": 250.00,
                    "payout": 0,
                    "status": "lost",
                    "selections": [
                        {
                            "eventName": "Jogo 1",
                            "outcomeName": "Time A",
                            "odds": 2.00,
                            "status": "won"
                        },
                        {
                            "eventName": "Jogo 2",
                            "outcomeName": "Time B",
                            "odds": 2.50,
                            "status": "lost"
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(BetType.MULTIPLE, result.betType)
            assertEquals(TicketStatus.LOST, result.ticketStatus)
            assertEquals(2, result.selections.size)
            assertEquals(SelectionStatus.WON, result.selections[0].status)
            assertEquals(SelectionStatus.LOST, result.selections[1].status)
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete com cashout")
        fun shouldParseCashoutTicket() {
            val json = """
            {
                "data": {
                    "ticketId": "TICKET-003",
                    "stake": 100.00,
                    "totalOdds": 3.00,
                    "potentialWin": 300.00,
                    "payout": 150.00,
                    "status": "cashout",
                    "selections": [
                        {
                            "eventName": "Jogo",
                            "outcomeName": "Empate",
                            "odds": 3.00,
                            "status": "cashout"
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(TicketStatus.CASHOUT, result.ticketStatus)
            assertEquals(BigDecimal("150.0"), result.actualPayout)
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete aberto")
        fun shouldParseOpenTicket() {
            val json = """
            {
                "data": {
                    "ticketId": "TICKET-004",
                    "stake": 100.00,
                    "totalOdds": 2.00,
                    "potentialWin": 200.00,
                    "status": "open",
                    "selections": [
                        {
                            "eventName": "Jogo Futuro",
                            "outcomeName": "Time X",
                            "odds": 2.00,
                            "status": "pending"
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(TicketStatus.OPEN, result.ticketStatus)
            assertNull(result.actualPayout)
            assertEquals(SelectionStatus.PENDING, result.selections[0].status)
        }
    }
}
