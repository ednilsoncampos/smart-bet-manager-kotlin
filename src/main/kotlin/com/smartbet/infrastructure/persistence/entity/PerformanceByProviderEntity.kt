package com.smartbet.infrastructure.persistence.entity

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal

/**
 * ID composto para PerformanceByProviderEntity
 */
@Embeddable
data class PerformanceByProviderId(
    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "provider_id", nullable = false)
    val providerId: Long = 0
) : Serializable

/**
 * Entidade JPA para a tabela analytics.performance_by_provider
 *
 * Armazena a performance do apostador por casa de apostas,
 * permitindo comparar resultados entre diferentes providers.
 */
@Entity
@Table(name = "performance_by_provider", schema = "analytics")
data class PerformanceByProviderEntity(
    @EmbeddedId
    val id: PerformanceByProviderId,

    // Contadores de tickets
    @Column(name = "total_tickets", nullable = false)
    var totalTickets: Int = 0,

    @Column(name = "wins", nullable = false)
    var wins: Int = 0,

    @Column(name = "losses", nullable = false)
    var losses: Int = 0,

    @Column(name = "voids", nullable = false)
    var voids: Int = 0,

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

    // Timestamp
    @Column(name = "last_settled_at", nullable = false)
    var lastSettledAt: Long = 0
)
