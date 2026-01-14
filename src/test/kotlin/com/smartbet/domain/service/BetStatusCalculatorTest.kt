package com.smartbet.domain.service

import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("BetStatusCalculator")
class BetStatusCalculatorTest {
    
    @Nested
    @DisplayName("calculate()")
    inner class CalculateTests {
        
        @Test
        @DisplayName("deve retornar PENDING quando bilhete está OPEN")
        fun shouldReturnPendingWhenTicketIsOpen() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = null,
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.OPEN
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.PENDING, result.financialStatus)
            assertEquals(BigDecimal.ZERO, result.profitLoss)
            assertEquals(BigDecimal.ZERO, result.roi)
        }
        
        @Test
        @DisplayName("deve retornar PENDING quando actualPayout é null")
        fun shouldReturnPendingWhenActualPayoutIsNull() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = null,
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.WON
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.PENDING, result.financialStatus)
        }
        
        @Test
        @DisplayName("deve retornar TOTAL_LOSS quando payout é zero")
        fun shouldReturnTotalLossWhenPayoutIsZero() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = BigDecimal.ZERO,
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.LOST
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.TOTAL_LOSS, result.financialStatus)
            assertEquals(BigDecimal("-100.00"), result.profitLoss)
            assertEquals(BigDecimal("-100.0000"), result.roi)
        }
        
        @Test
        @DisplayName("deve retornar PARTIAL_LOSS quando payout é menor que stake")
        fun shouldReturnPartialLossWhenPayoutIsLessThanStake() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = BigDecimal("50.00"),
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.CASHOUT
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.PARTIAL_LOSS, result.financialStatus)
            assertEquals(BigDecimal("-50.00"), result.profitLoss)
            assertEquals(BigDecimal("-50.0000"), result.roi)
        }
        
        @Test
        @DisplayName("deve retornar BREAK_EVEN quando payout é igual ao stake")
        fun shouldReturnBreakEvenWhenPayoutEqualsStake() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = BigDecimal("100.00"),
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.VOID
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.BREAK_EVEN, result.financialStatus)
            assertEquals(BigDecimal("0.00"), result.profitLoss)
            assertEquals(BigDecimal("0.0000"), result.roi)
        }
        
        @Test
        @DisplayName("deve retornar PARTIAL_WIN quando payout está entre stake e potencial")
        fun shouldReturnPartialWinWhenPayoutIsBetweenStakeAndPotential() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = BigDecimal("150.00"),
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.WON
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.PARTIAL_WIN, result.financialStatus)
            assertEquals(BigDecimal("50.00"), result.profitLoss)
            assertEquals(BigDecimal("50.0000"), result.roi)
        }
        
        @Test
        @DisplayName("deve retornar FULL_WIN quando payout é igual ou maior que potencial")
        fun shouldReturnFullWinWhenPayoutEqualsOrExceedsPotential() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = BigDecimal("250.00"),
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.WON
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.FULL_WIN, result.financialStatus)
            assertEquals(BigDecimal("150.00"), result.profitLoss)
            assertEquals(BigDecimal("150.0000"), result.roi)
        }
        
        @Test
        @DisplayName("deve retornar FULL_WIN quando payout excede potencial (bônus)")
        fun shouldReturnFullWinWhenPayoutExceedsPotential() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("100.00"),
                actualPayout = BigDecimal("300.00"),
                potentialPayout = BigDecimal("250.00"),
                ticketStatus = TicketStatus.WON
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(FinancialStatus.FULL_WIN, result.financialStatus)
            assertEquals(BigDecimal("200.00"), result.profitLoss)
        }
        
        @Test
        @DisplayName("deve calcular ROI corretamente")
        fun shouldCalculateRoiCorrectly() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal("50.00"),
                actualPayout = BigDecimal("125.00"),
                potentialPayout = BigDecimal("125.00"),
                ticketStatus = TicketStatus.WON
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            // ROI = (125 - 50) / 50 * 100 = 150%
            assertEquals(BigDecimal("150.0000"), result.roi)
        }
        
        @Test
        @DisplayName("deve retornar ROI zero quando stake é zero")
        fun shouldReturnZeroRoiWhenStakeIsZero() {
            val input = BetStatusCalculator.CalculateInput(
                stake = BigDecimal.ZERO,
                actualPayout = BigDecimal("100.00"),
                potentialPayout = BigDecimal("100.00"),
                ticketStatus = TicketStatus.WON
            )
            
            val result = BetStatusCalculator.calculate(input)
            
            assertEquals(BigDecimal.ZERO, result.roi)
        }
    }
    
    @Nested
    @DisplayName("calculateFromTicket()")
    inner class CalculateFromTicketTests {
        
        @Test
        @DisplayName("deve funcionar como wrapper para calculate()")
        fun shouldWorkAsWrapperForCalculate() {
            val result = BetStatusCalculator.calculateFromTicket(
                stake = BigDecimal("100.00"),
                actualPayout = BigDecimal("200.00"),
                potentialPayout = BigDecimal("200.00"),
                ticketStatus = TicketStatus.WON
            )
            
            assertEquals(FinancialStatus.FULL_WIN, result.financialStatus)
            assertEquals(BigDecimal("100.00"), result.profitLoss)
        }
    }
}
