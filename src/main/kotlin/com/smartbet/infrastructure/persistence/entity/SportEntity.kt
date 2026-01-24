package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.Sport
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

@Entity
@Table(name = "sports", schema = "betting")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class SportEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "provider_id", nullable = false)
    var providerId: Long = 0,

    @Column(name = "external_id", nullable = false)
    var externalId: Int = 0,

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Sport = Sport(
        id = id,
        providerId = providerId,
        externalId = externalId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(sport: Sport): SportEntity = SportEntity(
            id = sport.id,
            providerId = sport.providerId,
            externalId = sport.externalId,
            name = sport.name,
            createdAt = sport.createdAt,
            updatedAt = sport.updatedAt
        )
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = System.currentTimeMillis()
    }
}
