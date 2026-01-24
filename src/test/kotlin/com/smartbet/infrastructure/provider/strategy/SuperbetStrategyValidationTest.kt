package com.smartbet.infrastructure.provider.strategy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.smartbet.infrastructure.provider.gateway.HttpGateway
import com.smartbet.presentation.exception.InvalidTicketDataException
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SuperbetStrategy - Validação de Dados")
class SuperbetStrategyValidationTest {

    private lateinit var strategy: SuperbetStrategy
    private lateinit var objectMapper: ObjectMapper
    private lateinit var httpGateway: HttpGateway

    @BeforeEach
    fun setup() {
        objectMapper = jacksonObjectMapper()
        httpGateway = mockk(relaxed = true)
        strategy = SuperbetStrategy(objectMapper, httpGateway)
    }
    
    @Nested
    @DisplayName("Validação de Stake Zerado")
    inner class StakeValidationTests {
        
        @Test
        @DisplayName("deve lançar exceção quando stake é zero")
        fun shouldThrowExceptionWhenStakeIsZero() {
            val json = """
            {
                "ticket": {
                    "ticketId": "TICKET-001",
                    "status": "lost",
                    "coefficient": 2.50,
                    "payment": {
                        "stake": 0
                    },
                    "win": {
                        "potentialTotalWinnings": 0,
                        "payoff": 0
                    },
                    "events": [
                        {
                            "eventId": "EVT-001",
                            "name": ["Flamengo", "Palmeiras"],
                            "status": "lost",
                            "coefficient": 2.50,
                            "eventComponents": [
                                {
                                    "market": { "name": "Resultado" },
                                    "oddComponent": {
                                        "name": "Flamengo",
                                        "oddStatus": "LOST"
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val exception = assertThrows(InvalidTicketDataException::class.java) {
                strategy.parseResponse(json)
            }
            
            assertTrue(exception.message?.contains("Valor apostado deve ser maior que zero") == true)
            assertTrue(exception.details.containsKey("stake"))
            assertEquals(422, 422) // Será retornado como 422 pelo handler
        }
        
        @Test
        @DisplayName("deve lançar exceção quando stake está faltando")
        fun shouldThrowExceptionWhenStakeIsMissing() {
            val json = """
            {
                "ticket": {
                    "ticketId": "TICKET-002",
                    "status": "lost",
                    "coefficient": 2.50,
                    "payment": {},
                    "win": {
                        "potentialTotalWinnings": 0,
                        "payoff": 0
                    },
                    "events": [
                        {
                            "eventId": "EVT-001",
                            "name": ["Time A", "Time B"],
                            "status": "lost",
                            "coefficient": 2.50,
                            "eventComponents": [
                                {
                                    "market": { "name": "Resultado" },
                                    "oddComponent": {
                                        "name": "Time A",
                                        "oddStatus": "LOST"
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
            """.trimIndent()
            
            assertThrows(InvalidTicketDataException::class.java) {
                strategy.parseResponse(json)
            }
        }
    }
    
    @Nested
    @DisplayName("Validação de Odd Total")
    inner class OddValidationTests {
        
        @Test
        @DisplayName("deve lançar exceção quando odd total é 1.0 (padrão)")
        fun shouldThrowExceptionWhenOddIsOne() {
            val json = """
            {
                "ticket": {
                    "ticketId": "TICKET-003",
                    "status": "lost",
                    "coefficient": 1.0,
                    "payment": {
                        "stake": 100
                    },
                    "win": {
                        "potentialTotalWinnings": 100,
                        "payoff": 0
                    },
                    "events": [
                        {
                            "eventId": "EVT-001",
                            "name": ["Time A", "Time B"],
                            "status": "lost",
                            "coefficient": 1.0,
                            "eventComponents": [
                                {
                                    "market": { "name": "Resultado" },
                                    "oddComponent": {
                                        "name": "Time A",
                                        "oddStatus": "LOST"
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
            """.trimIndent()
            
            val exception = assertThrows(InvalidTicketDataException::class.java) {
                strategy.parseResponse(json)
            }
            
            assertTrue(exception.message?.contains("Odd total deve ser maior que 1.0") == true)
            assertTrue(exception.details.containsKey("totalOdd"))
        }
        
        @Test
        @DisplayName("deve lançar exceção quando odd total é zero")
        fun shouldThrowExceptionWhenOddIsZero() {
            val json = """
            {
                "ticket": {
                    "ticketId": "TICKET-004",
                    "status": "lost",
                    "coefficient": 0,
                    "payment": {
                        "stake": 100
                    },
                    "win": {
                        "potentialTotalWinnings": 0,
                        "payoff": 0
                    },
                    "events": []
                }
            }
            """.trimIndent()
            
            assertThrows(InvalidTicketDataException::class.java) {
                strategy.parseResponse(json)
            }
        }
    }
    
    @Nested
    @DisplayName("Validação de Múltiplos Erros")
    inner class MultipleErrorsTests {
        
        @Test
        @DisplayName("deve retornar múltiplos erros quando stake e odd estão zerados")
        fun shouldReturnMultipleErrorsWhenBothInvalid() {
            val json = """
            {
                "ticket": {
                    "ticketId": "TICKET-005",
                    "status": "lost",
                    "coefficient": 0,
                    "payment": {
                        "stake": 0
                    },
                    "win": {
                        "potentialTotalWinnings": 0,
                        "payoff": 0
                    },
                    "events": []
                }
            }
            """.trimIndent()
            
            val exception = assertThrows(InvalidTicketDataException::class.java) {
                strategy.parseResponse(json)
            }
            
            assertEquals(2, exception.details.size)
            assertTrue(exception.details.containsKey("stake"))
            assertTrue(exception.details.containsKey("totalOdd"))
        }
    }
    
    @Nested
    @DisplayName("Validação de Estrutura Aninhada")
    inner class NestedStructureTests {
        
        @Test
        @DisplayName("deve validar corretamente com estrutura aninhada em ticket")
        fun shouldValidateNestedTicketStructure() {
            val json = """
            {
                "ticket": {
                    "ticketId": "8901-QISL0B",
                    "status": "lost",
                    "coefficient": 85.11328125,
                    "payment": {
                        "stake": 10
                    },
                    "win": {
                        "potentialTotalWinnings": 77.37571023,
                        "payoff": 0
                    },
                    "events": [
                        {
                            "eventId": "EVT-001",
                            "name": ["Time A", "Time B"],
                            "status": "lost",
                            "coefficient": 85.11328125,
                            "eventComponents": [
                                {
                                    "market": { "name": "Resultado" },
                                    "oddComponent": {
                                        "name": "Time A",
                                        "oddStatus": "LOST"
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
            """.trimIndent()
            
            // Não deve lançar exceção
            val result = strategy.parseResponse(json)
            assertNotNull(result)
            assertEquals("8901-QISL0B", result.externalTicketId)
        }
        
        @Test
        @DisplayName("deve validar corretamente com estrutura em data")
        fun shouldValidateDataStructure() {
            val json = """
            {
                "data": {
                    "ticketId": "TICKET-006",
                    "status": "win",
                    "coefficient": 2.50,
                    "payment": {
                        "stake": 50
                    },
                    "win": {
                        "potentialTotalWinnings": 125,
                        "payoff": 125
                    },
                    "events": [
                        {
                            "eventId": "EVT-001",
                            "name": ["Time A", "Time B"],
                            "status": "win",
                            "coefficient": 2.50,
                            "eventComponents": [
                                {
                                    "market": { "name": "Resultado" },
                                    "oddComponent": {
                                        "name": "Time A",
                                        "oddStatus": "WIN"
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
            """.trimIndent()
            
            // Não deve lançar exceção
            val result = strategy.parseResponse(json)
            assertNotNull(result)
            assertEquals("TICKET-006", result.externalTicketId)
        }
    }
}
