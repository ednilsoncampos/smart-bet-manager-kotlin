package com.smartbet.infrastructure.persistence.entity

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal

/**
 * ID composto para PerformanceByMonthEntity
 */
@Embeddable
data class PerformanceByMonthId(
    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "year", nullable = false)
    val year: Int = 0,

    @Column(name = "month", nullable = false)
    val month: Int = 0
) : Serializable

/**
 * Entidade JPA para a tabela analytics.performance_by_month
 *
 * Armazena a performance mensal do apostador, permitindo consultas por período.
 */
@Entity
@Table(name = "performance_by_month", schema = "analytics")
data class PerformanceByMonthEntity(
    @EmbeddedId
    val id: PerformanceByMonthId,

    // Contadores de tickets
    @Column(name = "total_tickets", nullable = false)
    var totalTickets: Int = 0,

    @Column(name = "tickets_won", nullable = false)
    var ticketsWon: Int = 0,

    @Column(name = "tickets_lost", nullable = false)
    var ticketsLost: Int = 0,

    @Column(name = "tickets_void", nullable = false)
    var ticketsVoid: Int = 0,

    // Contadores granulares por FinancialStatus
    @Column(name = "tickets_full_won", nullable = false)
    var ticketsFullWon: Int = 0,

    @Column(name = "tickets_partial_won", nullable = false)
    var ticketsPartialWon: Int = 0,

    @Column(name = "tickets_break_even", nullable = false)
    var ticketsBreakEven: Int = 0,

    @Column(name = "tickets_partial_lost", nullable = false)
    var ticketsPartialLost: Int = 0,

    @Column(name = "tickets_total_lost", nullable = false)
    var ticketsTotalLost: Int = 0,

    // Métricas financeiras
    @Column(name = "total_stake", nullable = false, precision = 15, scale = 2)
    var totalStake: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_profit", nullable = false, precision = 15, scale = 2)
    var totalProfit: BigDecimal = BigDecimal.ZERO,

    // Métricas calculadas
    @Column(name = "roi", nullable = false, precision = 10, scale = 4)
    var roi: BigDecimal = BigDecimal.ZERO,

    @Column(name = "win_rate", nullable = false, precision = 5, scale = 2)
    var winRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "avg_stake", precision = 15, scale = 2)
    var avgStake: BigDecimal? = null,

    // Timestamps
    @Column(name = "first_bet_at")
    var firstBetAt: Long? = null,

    @Column(name = "last_settled_at", nullable = false)
    var lastSettledAt: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
)
