package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.Bankroll
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "bankrolls")
class BankrollEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,
    
    @Column(name = "provider_id")
    var providerId: Long? = null,
    
    @Column(nullable = false, length = 100)
    var name: String = "",
    
    @Column(nullable = false, length = 10)
    var currency: String = "BRL",
    
    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    var currentBalance: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "total_deposited", nullable = false, precision = 15, scale = 2)
    var totalDeposited: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "total_withdrawn", nullable = false, precision = 15, scale = 2)
    var totalWithdrawn: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "total_staked", nullable = false, precision = 15, scale = 2)
    var totalStaked: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "total_returns", nullable = false, precision = 15, scale = 2)
    var totalReturns: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Bankroll = Bankroll(
        id = id,
        userId = userId,
        providerId = providerId,
        name = name,
        currency = currency,
        currentBalance = currentBalance,
        totalDeposited = totalDeposited,
        totalWithdrawn = totalWithdrawn,
        totalStaked = totalStaked,
        totalReturns = totalReturns,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    companion object {
        fun fromDomain(bankroll: Bankroll): BankrollEntity = BankrollEntity(
            id = bankroll.id,
            userId = bankroll.userId,
            providerId = bankroll.providerId,
            name = bankroll.name,
            currency = bankroll.currency,
            currentBalance = bankroll.currentBalance,
            totalDeposited = bankroll.totalDeposited,
            totalWithdrawn = bankroll.totalWithdrawn,
            totalStaked = bankroll.totalStaked,
            totalReturns = bankroll.totalReturns,
            isActive = bankroll.isActive,
            createdAt = bankroll.createdAt,
            updatedAt = bankroll.updatedAt
        )
    }
    
    @PreUpdate
    fun preUpdate() {
        updatedAt = System.currentTimeMillis()
    }
}
