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
                "ticketId": "TICKET-001",
                "status": "win",
                "coefficient": 2.50,
                "dateReceived": "2024-01-15T10:30:00Z",
                "dateLastModified": "2024-01-15T12:00:00Z",
                "payment": {
                    "stake": 100.00
                },
                "win": {
                    "potentialTotalWinnings": 250.00,
                    "payoff": 250.00,
                    "totalWinnings": 250.00
                },
                "events": [
                    {
                        "eventId": "EVT-001",
                        "name": ["Flamengo", "Palmeiras"],
                        "date": "2024-01-15T16:00:00Z",
                        "status": "win",
                        "coefficient": 2.50,
                        "odd": {
                            "coefficient": 2.50,
                            "oddUuid": "uuid-001"
                        },
                        "eventComponents": [
                            {
                                "market": {
                                    "name": "Resultado Final"
                                },
                                "oddComponent": {
                                    "name": "Flamengo",
                                    "oddUuid": "uuid-comp-001",
                                    "oddStatus": "WIN"
                                },
                                "status": "WIN"
                            }
                        ]
                    }
                ]
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
            assertEquals("Resultado Final", selection.marketType)
            assertEquals("Flamengo", selection.selection)
            assertEquals(SelectionStatus.WON, selection.status)
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete múltiplo perdido")
        fun shouldParseLostMultipleTicket() {
            val json = """
            {
                "ticketId": "TICKET-002",
                "status": "lose",
                "coefficient": 5.00,
                "payment": {
                    "stake": 50.00
                },
                "win": {
                    "potentialTotalWinnings": 250.00,
                    "payoff": 0,
                    "totalWinnings": 0
                },
                "events": [
                    {
                        "eventId": "EVT-001",
                        "name": ["Time A", "Time B"],
                        "status": "win",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": {
                                    "name": "Time A",
                                    "oddStatus": "WIN"
                                }
                            }
                        ]
                    },
                    {
                        "eventId": "EVT-002",
                        "name": ["Time C", "Time D"],
                        "status": "lose",
                        "coefficient": 2.50,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": {
                                    "name": "Time C",
                                    "oddStatus": "LOST"
                                }
                            }
                        ]
                    }
                ]
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
                "ticketId": "TICKET-003",
                "status": "cashout",
                "coefficient": 3.00,
                "payment": {
                    "stake": 100.00
                },
                "win": {
                    "potentialTotalWinnings": 300.00,
                    "payoff": 150.00,
                    "isCashedOut": true
                },
                "events": [
                    {
                        "eventId": "EVT-001",
                        "name": ["Time X", "Time Y"],
                        "status": "cashout",
                        "coefficient": 3.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Empate" },
                                "oddComponent": {
                                    "name": "Empate",
                                    "oddStatus": "CASHOUT"
                                }
                            }
                        ]
                    }
                ]
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
                "ticketId": "TICKET-004",
                "status": "open",
                "coefficient": 2.00,
                "payment": {
                    "stake": 100.00
                },
                "win": {
                    "potentialTotalWinnings": 200.00
                },
                "events": [
                    {
                        "eventId": "EVT-001",
                        "name": ["Time Futuro", "Adversário"],
                        "status": "pending",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": {
                                    "name": "Time Futuro",
                                    "oddStatus": "PENDING"
                                }
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(TicketStatus.OPEN, result.ticketStatus)
            assertNull(result.actualPayout)
            assertEquals(SelectionStatus.PENDING, result.selections[0].status)
        }
        
        @Test
        @DisplayName("deve parsear resposta de bilhete sistema")
        fun shouldParseSystemTicket() {
            val json = """
            {
                "ticketId": "TICKET-005",
                "status": "win",
                "coefficient": 10.00,
                "system": {
                    "selected": [3],
                    "fixed": 0,
                    "count": 4
                },
                "payment": {
                    "stake": 100.00
                },
                "win": {
                    "potentialTotalWinnings": 1000.00,
                    "payoff": 500.00
                },
                "events": [
                    {
                        "name": ["Time A", "Time B"],
                        "status": "win",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": { "name": "Time A", "oddStatus": "WIN" }
                            }
                        ]
                    },
                    {
                        "name": ["Time C", "Time D"],
                        "status": "win",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": { "name": "Time C", "oddStatus": "WIN" }
                            }
                        ]
                    },
                    {
                        "name": ["Time E", "Time F"],
                        "status": "win",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": { "name": "Time E", "oddStatus": "WIN" }
                            }
                        ]
                    },
                    {
                        "name": ["Time G", "Time H"],
                        "status": "lose",
                        "coefficient": 2.50,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": { "name": "Time G", "oddStatus": "LOST" }
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(BetType.SYSTEM, result.betType)
            assertEquals("3/4", result.systemDescription)
            assertEquals(TicketStatus.WON, result.ticketStatus)
            assertEquals(4, result.selections.size)
        }
        
        @Test
        @DisplayName("deve parsear evento com múltiplos eventComponents")
        fun shouldParseEventWithMultipleComponents() {
            val json = """
            {
                "ticketId": "TICKET-006",
                "status": "win",
                "coefficient": 1.50,
                "payment": {
                    "stake": 100.00
                },
                "win": {
                    "potentialTotalWinnings": 150.00,
                    "payoff": 150.00
                },
                "events": [
                    {
                        "name": ["Bayern Munich", "Wolfsburg"],
                        "date": "2024-01-15T16:30:00Z",
                        "status": "win",
                        "coefficient": 1.50,
                        "eventComponents": [
                            {
                                "market": { "name": "Total de Gols" },
                                "oddComponent": {
                                    "name": "Mais de 2.5",
                                    "oddUuid": "uuid-1",
                                    "oddStatus": "WIN"
                                }
                            },
                            {
                                "market": { "name": "Ambas Marcam" },
                                "oddComponent": {
                                    "name": "Sim",
                                    "oddUuid": "uuid-2",
                                    "oddStatus": "WIN"
                                }
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(2, result.selections.size)
            
            val selection1 = result.selections[0]
            assertEquals("Bayern Munich x Wolfsburg", selection1.eventName)
            assertEquals("Total de Gols", selection1.marketType)
            assertEquals("Mais de 2.5", selection1.selection)
            
            val selection2 = result.selections[1]
            assertEquals("Bayern Munich x Wolfsburg", selection2.eventName)
            assertEquals("Ambas Marcam", selection2.marketType)
            assertEquals("Sim", selection2.selection)
        }
    }

    @Nested
    @DisplayName("Novos campos: sportId e isBetBuilder")
    inner class NewFieldsTests {
        
        @Test
        @DisplayName("deve extrair sportId do evento")
        fun shouldExtractSportIdFromEvent() {
            val json = """
            {
                "ticketId": "TICKET-SPORT-001",
                "status": "win",
                "coefficient": 2.00,
                "payment": { "stake": 100.00 },
                "win": { "potentialTotalWinnings": 200.00, "payoff": 200.00 },
                "events": [
                    {
                        "name": ["Flamengo", "Palmeiras"],
                        "status": "win",
                        "sportId": "5",
                        "tournamentId": "245",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado Final" },
                                "oddComponent": { "name": "Flamengo", "oddStatus": "WIN" }
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(1, result.selections.size)
            assertEquals("5", result.selections[0].sportId)
            assertEquals("245", result.selections[0].tournamentName)
        }
        
        @Test
        @DisplayName("deve detectar isBetBuilder quando há múltiplos eventComponents")
        fun shouldDetectBetBuilderWithMultipleComponents() {
            val json = """
            {
                "ticketId": "TICKET-BB-001",
                "status": "win",
                "coefficient": 3.50,
                "payment": { "stake": 50.00 },
                "win": { "potentialTotalWinnings": 175.00, "payoff": 175.00 },
                "events": [
                    {
                        "name": ["Bayern Munich", "Wolfsburg"],
                        "status": "win",
                        "sportId": "5",
                        "tournamentId": "245",
                        "coefficient": 3.50,
                        "eventComponents": [
                            {
                                "market": { "name": "Total de Gols" },
                                "oddComponent": { "name": "Mais de 2.5", "oddStatus": "WIN" }
                            },
                            {
                                "market": { "name": "Ambas Marcam" },
                                "oddComponent": { "name": "Sim", "oddStatus": "WIN" }
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(2, result.selections.size)
            // Ambas as seleções devem ser marcadas como Bet Builder
            assertTrue(result.selections[0].isBetBuilder)
            assertTrue(result.selections[1].isBetBuilder)
            assertEquals("5", result.selections[0].sportId)
            assertEquals("5", result.selections[1].sportId)
        }
        
        @Test
        @DisplayName("não deve marcar como isBetBuilder quando há apenas um eventComponent")
        fun shouldNotMarkAsBetBuilderWithSingleComponent() {
            val json = """
            {
                "ticketId": "TICKET-SINGLE-001",
                "status": "win",
                "coefficient": 2.00,
                "payment": { "stake": 100.00 },
                "win": { "potentialTotalWinnings": 200.00, "payoff": 200.00 },
                "events": [
                    {
                        "name": ["Time A", "Time B"],
                        "status": "win",
                        "sportId": "5",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado Final" },
                                "oddComponent": { "name": "Time A", "oddStatus": "WIN" }
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertEquals(1, result.selections.size)
            assertFalse(result.selections[0].isBetBuilder)
        }
        
        @Test
        @DisplayName("deve manter sportId nulo quando não presente no JSON")
        fun shouldKeepSportIdNullWhenNotPresent() {
            val json = """
            {
                "ticketId": "TICKET-NO-SPORT",
                "status": "win",
                "coefficient": 2.00,
                "payment": { "stake": 100.00 },
                "win": { "potentialTotalWinnings": 200.00, "payoff": 200.00 },
                "events": [
                    {
                        "name": ["Time A", "Time B"],
                        "status": "win",
                        "coefficient": 2.00,
                        "eventComponents": [
                            {
                                "market": { "name": "Resultado" },
                                "oddComponent": { "name": "Time A", "oddStatus": "WIN" }
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()
            
            val result = strategy.parseResponse(json)
            
            assertNull(result.selections[0].sportId)
            assertNull(result.selections[0].tournamentName)
        }
    }
}
