package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.BetTicket
import com.smartbet.domain.enum.BetSide
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "bet_tickets")
class BetTicketEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,
    
    @Column(name = "provider_id", nullable = false)
    var providerId: Long = 0,
    
    @Column(name = "bankroll_id")
    var bankrollId: Long? = null,
    
    @Column(name = "external_ticket_id", length = 100)
    var externalTicketId: String? = null,
    
    @Column(name = "source_url", length = 500)
    var sourceUrl: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bet_type", nullable = false, length = 50)
    var betType: BetType = BetType.SINGLE,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bet_side", nullable = false, length = 50)
    var betSide: BetSide = BetSide.BACK,
    
    @Column(nullable = false, precision = 15, scale = 2)
    var stake: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "total_odd", nullable = false, precision = 10, scale = 4)
    var totalOdd: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "potential_payout", precision = 15, scale = 2)
    var potentialPayout: BigDecimal? = null,
    
    @Column(name = "actual_payout", precision = 15, scale = 2)
    var actualPayout: BigDecimal? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false, length = 50)
    var ticketStatus: TicketStatus = TicketStatus.OPEN,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "financial_status", nullable = false, length = 50)
    var financialStatus: FinancialStatus = FinancialStatus.PENDING,
    
    @Column(name = "profit_loss", nullable = false, precision = 15, scale = 2)
    var profitLoss: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false, precision = 10, scale = 4)
    var roi: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "system_description", length = 50)
    var systemDescription: String? = null,
    
    @Column(name = "placed_at")
    var placedAt: Long? = null,
    
    @Column(name = "settled_at")
    var settledAt: Long? = null,
    
    @OneToMany(mappedBy = "ticket", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var selections: MutableList<BetSelectionEntity> = mutableListOf(),
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BetTicket = BetTicket(
        id = id,
        userId = userId,
        providerId = providerId,
        bankrollId = bankrollId,
        externalTicketId = externalTicketId,
        sourceUrl = sourceUrl,
        betType = betType,
        betSide = betSide,
        stake = stake,
        totalOdd = totalOdd,
        potentialPayout = potentialPayout,
        actualPayout = actualPayout,
        ticketStatus = ticketStatus,
        financialStatus = financialStatus,
        profitLoss = profitLoss,
        roi = roi,
        systemDescription = systemDescription,
        placedAt = placedAt,
        settledAt = settledAt,
        selections = selections.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    companion object {
        fun fromDomain(ticket: BetTicket): BetTicketEntity {
            val entity = BetTicketEntity(
                id = ticket.id,
                userId = ticket.userId,
                providerId = ticket.providerId,
                bankrollId = ticket.bankrollId,
                externalTicketId = ticket.externalTicketId,
                sourceUrl = ticket.sourceUrl,
                betType = ticket.betType,
                betSide = ticket.betSide,
                stake = ticket.stake,
                totalOdd = ticket.totalOdd,
                potentialPayout = ticket.potentialPayout,
                actualPayout = ticket.actualPayout,
                ticketStatus = ticket.ticketStatus,
                financialStatus = ticket.financialStatus,
                profitLoss = ticket.profitLoss,
                roi = ticket.roi,
                systemDescription = ticket.systemDescription,
                placedAt = ticket.placedAt,
                settledAt = ticket.settledAt,
                createdAt = ticket.createdAt,
                updatedAt = ticket.updatedAt
            )
            return entity
        }
    }
    
    @PreUpdate
    fun preUpdate() {
        updatedAt = System.currentTimeMillis()
    }
}
