package com.smartbet.application.usecase

import com.smartbet.application.dto.RefreshResult
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

@ExtendWith(MockKExtension::class)
@DisplayName("TicketRefreshService")
class TicketRefreshServiceTest {
    
    @MockK
    private lateinit var ticketService: TicketService
    
    @InjectMockKs
    private lateinit var ticketRefreshService: TicketRefreshService
    
    private val userId = 1L
    
    @BeforeEach
    fun setup() {
        clearAllMocks()
    }
    
    @Nested
    @DisplayName("refreshOpenTicketsAsync()")
    inner class RefreshOpenTicketsAsyncTests {
        
        @Test
        @DisplayName("deve retornar resultado do refresh com sucesso")
        fun shouldReturnRefreshResultSuccessfully() {
            // Arrange
            val expectedResult = RefreshResult(
                totalProcessed = 5,
                updated = 3,
                unchanged = 2,
                errors = 0
            )
            
            every { ticketService.refreshOpenTickets(userId) } returns expectedResult
            
            // Act
            val future = ticketRefreshService.refreshOpenTicketsAsync(userId)
            val result = future.get()
            
            // Assert
            assertEquals(5, result.totalProcessed)
            assertEquals(3, result.updated)
            assertEquals(2, result.unchanged)
            assertEquals(0, result.errors)
            
            verify(exactly = 1) { ticketService.refreshOpenTickets(userId) }
        }
        
        @Test
        @DisplayName("deve retornar future com erro quando refresh falha")
        fun shouldReturnFailedFutureWhenRefreshFails() {
            // Arrange
            every { ticketService.refreshOpenTickets(userId) } throws RuntimeException("Test error")
            
            // Act
            val future = ticketRefreshService.refreshOpenTicketsAsync(userId)
            
            // Assert
            assertTrue(future.isCompletedExceptionally)
        }
    }
}
