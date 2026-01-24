package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.BetSelectionComponent
import com.smartbet.domain.enum.SelectionStatus
import jakarta.persistence.*

@Entity
@Table(name = "bet_selection_components", schema = "betting")
class BetSelectionComponentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selection_id", nullable = false)
    var selection: BetSelectionEntity? = null,
    
    @Column(name = "market_id", length = 50)
    var marketId: String? = null,
    
    @Column(name = "market_name", nullable = false)
    var marketName: String = "",
    
    @Column(name = "selection_name", nullable = false)
    var selectionName: String = "",
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: SelectionStatus = SelectionStatus.PENDING,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BetSelectionComponent = BetSelectionComponent(
        id = id,
        selectionId = selection?.id ?: 0,
        marketId = marketId,
        marketName = marketName,
        selectionName = selectionName,
        status = status,
        createdAt = createdAt
    )
    
    companion object {
        fun fromDomain(
            component: BetSelectionComponent, 
            selectionEntity: BetSelectionEntity
        ): BetSelectionComponentEntity = BetSelectionComponentEntity(
            id = component.id,
            selection = selectionEntity,
            marketId = component.marketId,
            marketName = component.marketName,
            selectionName = component.selectionName,
            status = component.status,
            createdAt = component.createdAt
        )
    }
}
