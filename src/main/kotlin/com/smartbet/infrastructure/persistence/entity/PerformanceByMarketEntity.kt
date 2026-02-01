package com.smartbet.infrastructure.persistence.entity

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal

/**
 * ID composto para PerformanceByMarketEntity
 */
@Embeddable
data class PerformanceByMarketId(
    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "market_type", nullable = false, length = 100)
    val marketType: String = ""
) : Serializable

/**
 * Entidade JPA para a tabela analytics.performance_by_market
 *
 * Armazena a performance do apostador por tipo de mercado,
 * identificando mercados mais lucrativos ou deficitários.
 */
@Entity
@Table(name = "performance_by_market", schema = "analytics")
data class PerformanceByMarketEntity(
    @EmbeddedId
    val id: PerformanceByMarketId,

    // Contadores (baseados em seleções)
    @Column(name = "total_selections", nullable = false)
    var totalSelections: Int = 0,

    @Column(name = "wins", nullable = false)
    var wins: Int = 0,

    @Column(name = "losses", nullable = false)
    var losses: Int = 0,

    @Column(name = "voids", nullable = false)
    var voids: Int = 0,

    // Tickets únicos que incluem esse mercado
    @Column(name = "unique_tickets", nullable = false)
    var uniqueTickets: Int = 0,

    // Contadores granulares por FinancialStatus dos tickets
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

    @Column(name = "success_rate", nullable = false, precision = 5, scale = 2)
    var successRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "avg_odd", precision = 10, scale = 4)
    var avgOdd: BigDecimal? = null,

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
