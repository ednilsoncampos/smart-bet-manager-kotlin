package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.BankrollTransaction
import com.smartbet.domain.enum.TransactionType
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "bankroll_transactions")
class BankrollTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "bankroll_id", nullable = false)
    var bankrollId: Long = 0,
    
    @Column(name = "ticket_id")
    var ticketId: Long? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var type: TransactionType = TransactionType.ADJUSTMENT,
    
    @Column(nullable = false, precision = 15, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    var balanceAfter: BigDecimal = BigDecimal.ZERO,
    
    @Column(length = 255)
    var description: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BankrollTransaction = BankrollTransaction(
        id = id,
        bankrollId = bankrollId,
        ticketId = ticketId,
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        description = description,
        createdAt = createdAt
    )
    
    companion object {
        fun fromDomain(transaction: BankrollTransaction): BankrollTransactionEntity = BankrollTransactionEntity(
            id = transaction.id,
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
