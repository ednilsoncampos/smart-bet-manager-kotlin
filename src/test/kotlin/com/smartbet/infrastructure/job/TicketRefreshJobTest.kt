package com.smartbet.infrastructure.job

import com.smartbet.application.dto.RefreshError
import com.smartbet.application.dto.RefreshResult
import com.smartbet.application.usecase.TicketService
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("TicketRefreshJob")
class TicketRefreshJobTest {
    
    @MockK
    private lateinit var ticketService: TicketService
    
    @InjectMockKs
    private lateinit var ticketRefreshJob: TicketRefreshJob
    
    @BeforeEach
    fun setup() {
        clearAllMocks()
    }
    
    @Nested
    @DisplayName("refreshAllOpenTickets()")
    inner class RefreshAllOpenTicketsTests {
        
        @Test
        @DisplayName("deve executar refresh com sucesso")
        fun shouldExecuteRefreshSuccessfully() {
            // Arrange
            val result = RefreshResult(
                totalProcessed = 10,
                updated = 5,
                unchanged = 5,
                errors = 0
            )
            
            every { ticketService.refreshAllOpenTickets() } returns result
            
            // Act
            ticketRefreshJob.refreshAllOpenTickets()
            
            // Assert
            verify(exactly = 1) { ticketService.refreshAllOpenTickets() }
        }
        
        @Test
        @DisplayName("deve logar erros quando houver falhas")
        fun shouldLogErrorsWhenThereAreFailures() {
            // Arrange
            val result = RefreshResult(
                totalProcessed = 10,
                updated = 7,
                unchanged = 1,
                errors = 2,
                errorDetails = listOf(
                    RefreshError(1L, "EXT-001", "Connection timeout"),
                    RefreshError(2L, "EXT-002", "Invalid response")
                )
            )
            
            every { ticketService.refreshAllOpenTickets() } returns result
            
            // Act
            ticketRefreshJob.refreshAllOpenTickets()
            
            // Assert
            verify(exactly = 1) { ticketService.refreshAllOpenTickets() }
        }
        
        @Test
        @DisplayName("deve capturar exceção e não propagar")
        fun shouldCatchExceptionAndNotPropagate() {
            // Arrange
            every { ticketService.refreshAllOpenTickets() } throws RuntimeException("Database error")
            
            // Act - não deve lançar exceção
            ticketRefreshJob.refreshAllOpenTickets()
            
            // Assert
            verify(exactly = 1) { ticketService.refreshAllOpenTickets() }
        }
    }
}
