package com.smartbet.infrastructure.persistence.entity

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Entidade JPA para a tabela analytics.performance_overall
 *
 * Armazena a performance geral (all-time) do apostador com métricas agregadas
 * e dados de gamificação (streaks, records).
 */
@Entity
@Table(name = "performance_overall", schema = "analytics")
data class PerformanceOverallEntity(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    // Contadores de tickets
    @Column(name = "total_tickets", nullable = false)
    var totalTickets: Int = 0,

    @Column(name = "tickets_won", nullable = false)
    var ticketsWon: Int = 0,

    @Column(name = "tickets_lost", nullable = false)
    var ticketsLost: Int = 0,

    @Column(name = "tickets_void", nullable = false)
    var ticketsVoid: Int = 0,

    @Column(name = "tickets_cashed_out", nullable = false)
    var ticketsCashedOut: Int = 0,

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

    @Column(name = "total_return", nullable = false, precision = 15, scale = 2)
    var totalReturn: BigDecimal = BigDecimal.ZERO,

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

    @Column(name = "avg_stake", precision = 15, scale = 2)
    var avgStake: BigDecimal? = null,

    // Gamificação: Streaks
    @Column(name = "current_streak")
    var currentStreak: Int = 0,

    @Column(name = "best_win_streak")
    var bestWinStreak: Int = 0,

    @Column(name = "worst_loss_streak")
    var worstLossStreak: Int = 0,

    // Gamificação: Records
    @Column(name = "biggest_win", precision = 15, scale = 2)
    var biggestWin: BigDecimal? = null,

    @Column(name = "biggest_loss", precision = 15, scale = 2)
    var biggestLoss: BigDecimal? = null,

    @Column(name = "best_roi_ticket", precision = 10, scale = 4)
    var bestRoiTicket: BigDecimal? = null,

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
