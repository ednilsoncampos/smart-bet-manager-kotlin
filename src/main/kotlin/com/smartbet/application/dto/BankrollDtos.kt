package com.smartbet.application.dto

import com.smartbet.domain.entity.Bankroll
import com.smartbet.domain.entity.BankrollTransaction
import com.smartbet.domain.enum.TransactionType
import java.math.BigDecimal
import java.time.Instant

// ============================================
// Request DTOs
// ============================================

data class CreateBankrollRequest(
    val name: String,
    val providerId: Long? = null,
    val currency: String = "BRL",
    val initialDeposit: BigDecimal? = null
)

data class RecordTransactionRequest(
    val bankrollId: Long,
    val type: TransactionType,
    val amount: BigDecimal,
    val description: String? = null
)

// ============================================
// Response DTOs
// ============================================

data class BankrollResponse(
    val id: Long,
    val providerId: Long?,
    val providerName: String?,
    val name: String,
    val currency: String,
    val currentBalance: BigDecimal,
    val totalDeposited: BigDecimal,
    val totalWithdrawn: BigDecimal,
    val totalStaked: BigDecimal,
    val totalReturns: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun fromDomain(bankroll: Bankroll, providerName: String? = null): BankrollResponse {
            return BankrollResponse(
                id = bankroll.id!!,
                providerId = bankroll.providerId,
                providerName = providerName,
                name = bankroll.name,
                currency = bankroll.currency,
                currentBalance = bankroll.currentBalance,
                totalDeposited = bankroll.totalDeposited,
                totalWithdrawn = bankroll.totalWithdrawn,
                totalStaked = bankroll.totalStaked,
                totalReturns = bankroll.totalReturns,
                profitLoss = bankroll.calculateProfitLoss(),
                roi = bankroll.calculateRoi(),
                isActive = bankroll.isActive,
                createdAt = bankroll.createdAt,
                updatedAt = bankroll.updatedAt
            )
        }
    }
}

data class TransactionResponse(
    val id: Long,
    val bankrollId: Long,
    val ticketId: Long?,
    val type: TransactionType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val description: String?,
    val createdAt: Instant
) {
    companion object {
        fun fromDomain(transaction: BankrollTransaction): TransactionResponse {
            return TransactionResponse(
                id = transaction.id!!,
                bankrollId = transaction.bankrollId,
                ticketId = transaction.ticketId,
                type = transaction.type,
                amount = transaction.amount,
                balanceAfter = transaction.balanceAfter,
                description = transaction.description,
                createdAt = transaction.createdAt
            )
        }
    }
}

data class BankrollSummaryResponse(
    val totalBalance: BigDecimal,
    val totalDeposited: BigDecimal,
    val totalWithdrawn: BigDecimal,
    val totalStaked: BigDecimal,
    val totalReturns: BigDecimal,
    val overallProfitLoss: BigDecimal,
    val overallRoi: BigDecimal,
    val bankrollCount: Int
)
