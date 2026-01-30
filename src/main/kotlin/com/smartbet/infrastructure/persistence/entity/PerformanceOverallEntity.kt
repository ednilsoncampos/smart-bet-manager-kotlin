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

    @Column(name = "avg_odd", precision = 10, scale = 4)
    var avgOdd: BigDecimal? = null,

    // Gamificação: Streaks
    @Column(name = "current_win_streak", nullable = false)
    var currentWinStreak: Int = 0,

    @Column(name = "current_loss_streak", nullable = false)
    var currentLossStreak: Int = 0,

    @Column(name = "max_win_streak", nullable = false)
    var maxWinStreak: Int = 0,

    @Column(name = "max_loss_streak", nullable = false)
    var maxLossStreak: Int = 0,

    // Gamificação: Records
    @Column(name = "biggest_win", precision = 15, scale = 2)
    var biggestWin: BigDecimal? = null,

    @Column(name = "biggest_loss", precision = 15, scale = 2)
    var biggestLoss: BigDecimal? = null,

    @Column(name = "highest_odd_won", precision = 10, scale = 4)
    var highestOddWon: BigDecimal? = null,

    // Timestamps
    @Column(name = "first_bet_at")
    var firstBetAt: Long? = null,

    @Column(name = "last_settled_at", nullable = false)
    var lastSettledAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
)
