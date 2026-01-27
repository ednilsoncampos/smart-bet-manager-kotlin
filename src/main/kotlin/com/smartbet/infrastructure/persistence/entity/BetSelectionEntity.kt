package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.BetSelection
import com.smartbet.domain.enum.SelectionStatus
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "bet_selections", schema = "betting")
class BetSelectionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    var ticket: BetTicketEntity? = null,
    
    @Column(name = "external_selection_id", length = 100)
    var externalSelectionId: String? = null,
    
    @Column(name = "event_name", nullable = false)
    var eventName: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    var tournament: TournamentEntity? = null,
    
    @Column(name = "market_type", length = 100)
    var marketType: String? = null,
    
    @Column(nullable = false)
    var selection: String = "",
    
    @Column(nullable = false, precision = 10, scale = 4)
    var odd: BigDecimal = BigDecimal.ZERO,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: SelectionStatus = SelectionStatus.PENDING,
    
    @Column(name = "event_date")
    var eventDate: Long? = null,
    
    @Column(name = "event_result", length = 100)
    var eventResult: String? = null,
    
    @Column(name = "sport_id", length = 50)
    var sportId: String? = null,
    
    @Column(name = "is_bet_builder")
    var isBetBuilder: Boolean = false,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BetSelection = BetSelection(
        id = id,
        ticketId = ticket?.id ?: 0,
        externalSelectionId = externalSelectionId,
        eventName = eventName,
        tournamentId = tournament?.id,
        tournamentName = tournament?.name,
        tournamentLocalName = tournament?.localName,
        marketType = marketType,
        selection = selection,
        odd = odd,
        status = status,
        eventDate = eventDate,
        eventResult = eventResult,
        sportId = sportId,
        isBetBuilder = isBetBuilder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(
            selection: BetSelection,
            ticketEntity: BetTicketEntity,
            tournamentEntity: TournamentEntity? = null
        ): BetSelectionEntity = BetSelectionEntity(
            id = selection.id,
            ticket = ticketEntity,
            externalSelectionId = selection.externalSelectionId,
            eventName = selection.eventName,
            tournament = tournamentEntity,
            marketType = selection.marketType,
            selection = selection.selection,
            odd = selection.odd,
            status = selection.status,
            eventDate = selection.eventDate,
            eventResult = selection.eventResult,
            sportId = selection.sportId,
            isBetBuilder = selection.isBetBuilder,
            createdAt = selection.createdAt,
            updatedAt = selection.updatedAt
        )
    }
    
    @PreUpdate
    fun preUpdate() {
        updatedAt = System.currentTimeMillis()
    }
}
