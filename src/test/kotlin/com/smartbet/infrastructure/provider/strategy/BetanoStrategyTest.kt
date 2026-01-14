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
        @DisplayName("deve parsear resposta de bilhete simples ganho")
        fun shouldParseWonSingleTicket() {
            val json = """
            {
                "data": {
                    "betslipId": "BET-001",
                    "stake": 100.00,
                    "totalOdds": 1.80,
                    "potentialWinnings": 180.00,
                    "payout": 180.00,
                    "status": "won",
                    "placedAt": "2024-01-15T10:30:00Z",
                    "settledAt": "2024-01-15T12:00:00Z",
                    "legs": [
                        {
                            "legId": "LEG-001",
                            "eventName": "Corinthians x São Paulo",
                            "competitionName": "Brasileirão Série A",
                            "marketName": "Ambas Marcam",
                            "outcomeName": "Sim",
                            "odds": 1.80,
                            "status": "won",
                            "eventDate": "2024-01-15T16:00:00Z",
                            "result": "2-1"
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals("BET-001", result.externalTicketId)
            assertEquals(BetType.SINGLE, result.betType)
            assertEquals(BigDecimal("100.0"), result.stake)
            assertEquals(BigDecimal("1.8"), result.totalOdd)
            assertEquals(BigDecimal("180.0"), result.potentialPayout)
            assertEquals(BigDecimal("180.0"), result.actualPayout)
            assertEquals(TicketStatus.WON, result.ticketStatus)
            assertEquals(1, result.selections.size)
            
            val selection = result.selections[0]
            assertEquals("Corinthians x São Paulo", selection.eventName)
            assertEquals("Brasileirão Série A", selection.tournamentName)
            assertEquals("Ambas Marcam", selection.marketType)
            assertEquals("Sim", selection.selection)
            assertEquals(BigDecimal("1.8"), selection.odd)
            assertEquals(SelectionStatus.WON, selection.status)
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete múltiplo perdido")
        fun shouldParseLostMultipleTicket() {
            val json = """
            {
                "data": {
                    "betslipId": "BET-002",
                    "stake": 50.00,
                    "totalOdds": 4.50,
                    "potentialWinnings": 225.00,
                    "payout": 0,
                    "status": "lost",
                    "legs": [
                        {
                            "eventName": "Jogo 1",
                            "outcomeName": "Over 2.5",
                            "odds": 1.80,
                            "status": "won"
                        },
                        {
                            "eventName": "Jogo 2",
                            "outcomeName": "Under 2.5",
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
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete anulado")
        fun shouldParseVoidTicket() {
            val json = """
            {
                "data": {
                    "betslipId": "BET-003",
                    "stake": 100.00,
                    "totalOdds": 2.00,
                    "potentialWinnings": 200.00,
                    "payout": 100.00,
                    "status": "void",
                    "legs": [
                        {
                            "eventName": "Jogo Cancelado",
                            "outcomeName": "Time A",
                            "odds": 2.00,
                            "status": "void"
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(TicketStatus.VOID, result.ticketStatus)
            assertEquals(BigDecimal("100.0"), result.actualPayout)
            assertEquals(SelectionStatus.VOID, result.selections[0].status)
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete aberto")
        fun shouldParseOpenTicket() {
            val json = """
            {
                "data": {
                    "betslipId": "BET-004",
                    "stake": 75.00,
                    "totalOdds": 3.00,
                    "potentialWinnings": 225.00,
                    "status": "active",
                    "legs": [
                        {
                            "eventName": "Jogo Futuro",
                            "outcomeName": "Empate",
                            "odds": 3.00,
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
        
        @Test
        @DisplayName("deve parsear resposta com estrutura alternativa (betslip)")
        fun shouldParseAlternativeStructure() {
            val json = """
            {
                "betslip": {
                    "id": "BET-005",
                    "amount": 100.00,
                    "combinedOdds": 2.50,
                    "maxWin": 250.00,
                    "winnings": 250.00,
                    "betStatus": "won",
                    "selections": [
                        {
                            "id": "SEL-001",
                            "matchName": "Time A vs Time B",
                            "leagueName": "Liga X",
                            "betTypeName": "1X2",
                            "selectionName": "Time A",
                            "price": 2.50,
                            "legStatus": "won"
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals("BET-005", result.externalTicketId)
            assertEquals(BigDecimal("100.0"), result.stake)
            assertEquals(BigDecimal("2.5"), result.totalOdd)
            assertEquals(TicketStatus.WON, result.ticketStatus)
        }
    }
}
