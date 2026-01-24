package com.smartbet.application.usecase

import com.smartbet.application.dto.CreateManualTicketRequest
import com.smartbet.application.dto.CreateSelectionRequest
import com.smartbet.application.dto.ListTicketsRequest
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.entity.BetSelectionEntity
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
import com.smartbet.infrastructure.persistence.entity.BettingProviderEntity
import com.smartbet.infrastructure.persistence.repository.BetSelectionComponentRepository
import com.smartbet.infrastructure.persistence.repository.BetSelectionRepository
import com.smartbet.infrastructure.persistence.repository.BetTicketRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import com.smartbet.infrastructure.persistence.repository.TournamentRepository
import com.smartbet.infrastructure.provider.gateway.HttpGateway
import com.smartbet.infrastructure.provider.strategy.BettingProviderFactory
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.*

@ExtendWith(MockKExtension::class)
@DisplayName("TicketService")
class TicketServiceTest {

    @MockK
    private lateinit var ticketRepository: BetTicketRepository

    @MockK
    private lateinit var selectionRepository: BetSelectionRepository

    @MockK
    private lateinit var selectionComponentRepository: BetSelectionComponentRepository

    @MockK
    private lateinit var providerRepository: BettingProviderRepository

    @MockK
    private lateinit var tournamentRepository: TournamentRepository

    @MockK
    private lateinit var providerFactory: BettingProviderFactory

    @MockK
    private lateinit var httpGateway: HttpGateway

    @InjectMockKs
    private lateinit var ticketService: TicketService
    
    private val userId = 1L
    private val providerId = 1L
    
    @BeforeEach
    fun setup() {
        clearAllMocks()
    }
    
    @Nested
    @DisplayName("createManual()")
    inner class CreateManualTests {
        
        @Test
        @DisplayName("deve criar bilhete manual com sucesso")
        fun shouldCreateManualTicketSuccessfully() {
            // Arrange
            val provider = BettingProviderEntity(
                id = providerId,
                slug = "superbet",
                name = "Superbet"
            )
            
            val request = CreateManualTicketRequest(
                providerId = providerId,
                betType = BetType.SINGLE,
                stake = BigDecimal("100.00"),
                totalOdd = BigDecimal("2.50"),
                selections = listOf(
                    CreateSelectionRequest(
                        eventName = "Flamengo x Palmeiras",
                        tournamentId = null, // Sem torneio associado
                        marketType = "Resultado Final",
                        selection = "Flamengo",
                        odd = BigDecimal("2.50")
                    )
                )
            )
            
            val savedTicket = BetTicketEntity(
                id = 1L,
                userId = userId,
                providerId = providerId,
                betType = BetType.SINGLE,
                stake = BigDecimal("100.00"),
                totalOdd = BigDecimal("2.50"),
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.OPEN,
                financialStatus = FinancialStatus.PENDING,
                selections = mutableListOf()
            )
            
            every { providerRepository.findById(providerId) } returns Optional.of(provider)
            every { ticketRepository.save(any()) } returns savedTicket
            every { selectionRepository.saveAll(any<List<BetSelectionEntity>>()) } returns emptyList()
            
            // Act
            val result = ticketService.createManual(userId, request)
            
            // Assert
            assertNotNull(result)
            assertEquals(1L, result.id)
            assertEquals(BetType.SINGLE, result.betType)
            assertEquals(BigDecimal("100.00"), result.stake)
            assertEquals(TicketStatus.OPEN, result.ticketStatus)
            assertEquals(FinancialStatus.PENDING, result.financialStatus)
            assertEquals("Superbet", result.providerName)
            
            verify(exactly = 1) { providerRepository.findById(providerId) }
            verify(exactly = 1) { ticketRepository.save(any()) }
        }
        
        @Test
        @DisplayName("deve lançar exceção quando provider não existe")
        fun shouldThrowExceptionWhenProviderNotFound() {
            // Arrange
            val request = CreateManualTicketRequest(
                providerId = 999L,
                betType = BetType.SINGLE,
                stake = BigDecimal("100.00"),
                totalOdd = BigDecimal("2.50"),
                selections = emptyList()
            )
            
            every { providerRepository.findById(999L) } returns Optional.empty()
            
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                ticketService.createManual(userId, request)
            }
        }
    }
    
    @Nested
    @DisplayName("listTickets()")
    inner class ListTicketsTests {
        
        @Test
        @DisplayName("deve listar bilhetes com paginação")
        fun shouldListTicketsWithPagination() {
            // Arrange
            val request = ListTicketsRequest(page = 0, pageSize = 10)
            
            val tickets = listOf(
                BetTicketEntity(
                    id = 1L,
                    userId = userId,
                    providerId = providerId,
                    stake = BigDecimal("100.00"),
                    totalOdd = BigDecimal("2.00"),
                    ticketStatus = TicketStatus.WON,
                    financialStatus = FinancialStatus.FULL_WIN
                ),
                BetTicketEntity(
                    id = 2L,
                    userId = userId,
                    providerId = providerId,
                    stake = BigDecimal("50.00"),
                    totalOdd = BigDecimal("3.00"),
                    ticketStatus = TicketStatus.LOST,
                    financialStatus = FinancialStatus.TOTAL_LOSS
                )
            )
            
            val page = PageImpl(tickets, PageRequest.of(0, 10), 2)
            
            val provider = BettingProviderEntity(
                id = providerId,
                slug = "superbet",
                name = "Superbet"
            )
            
            every { 
                ticketRepository.findByFilters(
                    userId = userId,
                    status = null,
                    financialStatus = null,
                    providerId = null,
                    pageable = any()
                ) 
            } returns page
            
            every { providerRepository.findAll() } returns listOf(provider)
            
            // Act
            val result = ticketService.listTickets(userId, request)
            
            // Assert
            assertEquals(2, result.content.size)
            assertEquals(0, result.page)
            assertEquals(10, result.pageSize)
            assertEquals(2, result.totalElements)
        }
        
        @Test
        @DisplayName("deve filtrar por status")
        fun shouldFilterByStatus() {
            // Arrange
            val request = ListTicketsRequest(
                status = TicketStatus.WON,
                page = 0,
                pageSize = 10
            )
            
            val page = PageImpl(emptyList<BetTicketEntity>(), PageRequest.of(0, 10), 0)
            
            every { 
                ticketRepository.findByFilters(
                    userId = userId,
                    status = TicketStatus.WON,
                    financialStatus = null,
                    providerId = null,
                    pageable = any()
                ) 
            } returns page
            
            every { providerRepository.findAll() } returns emptyList()
            
            // Act
            val result = ticketService.listTickets(userId, request)
            
            // Assert
            assertTrue(result.content.isEmpty())
            
            verify { 
                ticketRepository.findByFilters(
                    userId = userId,
                    status = TicketStatus.WON,
                    financialStatus = null,
                    providerId = null,
                    pageable = any()
                )
            }
        }
    }
    
    @Nested
    @DisplayName("getById()")
    inner class GetByIdTests {
        
        @Test
        @DisplayName("deve retornar bilhete por ID")
        fun shouldReturnTicketById() {
            // Arrange
            val ticketId = 1L
            val ticket = BetTicketEntity(
                id = ticketId,
                userId = userId,
                providerId = providerId,
                stake = BigDecimal("100.00"),
                totalOdd = BigDecimal("2.00")
            )
            
            val provider = BettingProviderEntity(
                id = providerId,
                slug = "superbet",
                name = "Superbet"
            )
            
            every { ticketRepository.findById(ticketId) } returns Optional.of(ticket)
            every { providerRepository.findById(providerId) } returns Optional.of(provider)
            
            // Act
            val result = ticketService.getById(userId, ticketId)
            
            // Assert
            assertEquals(ticketId, result.id)
            assertEquals("Superbet", result.providerName)
        }
        
        @Test
        @DisplayName("deve lançar exceção quando bilhete não existe")
        fun shouldThrowExceptionWhenTicketNotFound() {
            // Arrange
            every { ticketRepository.findById(999L) } returns Optional.empty()
            
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                ticketService.getById(userId, 999L)
            }
        }
        
        @Test
        @DisplayName("deve lançar exceção quando bilhete pertence a outro usuário")
        fun shouldThrowExceptionWhenTicketBelongsToAnotherUser() {
            // Arrange
            val ticket = BetTicketEntity(
                id = 1L,
                userId = 999L, // Outro usuário
                providerId = providerId,
                stake = BigDecimal("100.00"),
                totalOdd = BigDecimal("2.00")
            )
            
            every { ticketRepository.findById(1L) } returns Optional.of(ticket)
            
            // Act & Assert
            assertThrows(IllegalAccessException::class.java) {
                ticketService.getById(userId, 1L)
            }
        }
    }
    
    @Nested
    @DisplayName("delete()")
    inner class DeleteTests {
        
        @Test
        @DisplayName("deve deletar bilhete com sucesso")
        fun shouldDeleteTicketSuccessfully() {
            // Arrange
            val ticketId = 1L
            val ticket = BetTicketEntity(
                id = ticketId,
                userId = userId,
                providerId = providerId,
                stake = BigDecimal("100.00"),
                totalOdd = BigDecimal("2.00")
            )
            
            every { ticketRepository.findById(ticketId) } returns Optional.of(ticket)
            every { ticketRepository.delete(ticket) } just runs
            
            // Act
            ticketService.delete(userId, ticketId)
            
            // Assert
            verify(exactly = 1) { ticketRepository.delete(ticket) }
        }
    }
    
    @Nested
    @DisplayName("countOpenTicketsToRefresh()")
    inner class CountOpenTicketsToRefreshTests {
        
        @Test
        @DisplayName("deve retornar quantidade de bilhetes em aberto")
        fun shouldReturnCountOfOpenTickets() {
            // Arrange
            val openTickets = listOf(
                BetTicketEntity(
                    id = 1L,
                    userId = userId,
                    providerId = providerId,
                    stake = BigDecimal("100.00"),
                    totalOdd = BigDecimal("2.00"),
                    ticketStatus = TicketStatus.OPEN,
                    sourceUrl = "https://superbet.com/ticket/123"
                ),
                BetTicketEntity(
                    id = 2L,
                    userId = userId,
                    providerId = providerId,
                    stake = BigDecimal("50.00"),
                    totalOdd = BigDecimal("3.00"),
                    ticketStatus = TicketStatus.OPEN,
                    sourceUrl = "https://superbet.com/ticket/456"
                )
            )
            
            every { ticketRepository.findOpenTicketsByUserId(userId) } returns openTickets
            
            // Act
            val result = ticketService.countOpenTicketsToRefresh(userId)
            
            // Assert
            assertEquals(2, result)
        }
        
        @Test
        @DisplayName("deve retornar zero quando não há bilhetes em aberto")
        fun shouldReturnZeroWhenNoOpenTickets() {
            // Arrange
            every { ticketRepository.findOpenTicketsByUserId(userId) } returns emptyList()
            
            // Act
            val result = ticketService.countOpenTicketsToRefresh(userId)
            
            // Assert
            assertEquals(0, result)
        }
    }
    
    @Nested
    @DisplayName("refreshOpenTickets()")
    inner class RefreshOpenTicketsTests {
        
        @Test
        @DisplayName("deve retornar resultado vazio quando não há bilhetes em aberto")
        fun shouldReturnEmptyResultWhenNoOpenTickets() {
            // Arrange
            every { ticketRepository.findOpenTicketsByUserId(userId) } returns emptyList()
            
            // Act
            val result = ticketService.refreshOpenTickets(userId)
            
            // Assert
            assertEquals(0, result.totalProcessed)
            assertEquals(0, result.updated)
            assertEquals(0, result.unchanged)
            assertEquals(0, result.errors)
        }
        
        @Test
        @DisplayName("deve processar bilhetes em aberto e contar erros")
        fun shouldProcessOpenTicketsAndCountErrors() {
            // Arrange
            val ticketWithoutUrl = BetTicketEntity(
                id = 1L,
                userId = userId,
                providerId = providerId,
                stake = BigDecimal("100.00"),
                totalOdd = BigDecimal("2.00"),
                ticketStatus = TicketStatus.OPEN,
                sourceUrl = null // Sem URL - vai gerar erro
            )
            
            every { ticketRepository.findOpenTicketsByUserId(userId) } returns listOf(ticketWithoutUrl)
            
            // Act
            val result = ticketService.refreshOpenTickets(userId)
            
            // Assert
            assertEquals(1, result.totalProcessed)
            assertEquals(0, result.updated)
            assertEquals(0, result.unchanged)
            assertEquals(1, result.errors)
            assertEquals(1, result.errorDetails.size)
            assertTrue(result.errorDetails[0].errorMessage.contains("sourceUrl"))
        }
    }
    
    @Nested
    @DisplayName("refreshAllOpenTickets()")
    inner class RefreshAllOpenTicketsTests {
        
        @Test
        @DisplayName("deve retornar resultado vazio quando não há bilhetes em aberto")
        fun shouldReturnEmptyResultWhenNoOpenTickets() {
            // Arrange
            every { ticketRepository.findAllOpenTicketsWithSourceUrl() } returns emptyList()
            
            // Act
            val result = ticketService.refreshAllOpenTickets()
            
            // Assert
            assertEquals(0, result.totalProcessed)
            assertEquals(0, result.updated)
            assertEquals(0, result.unchanged)
            assertEquals(0, result.errors)
        }
    }
}
