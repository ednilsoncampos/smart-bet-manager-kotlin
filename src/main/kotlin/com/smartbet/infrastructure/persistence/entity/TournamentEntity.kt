package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.Tournament
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

@Entity
@Table(name = "tournaments", schema = "betting")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class TournamentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "provider_id", nullable = false)
    var providerId: Long = 0,

    @Column(name = "sport_id", nullable = false)
    var sportId: Long = 0,

    @Column(name = "external_id", nullable = false)
    var externalId: Int = 0,

    @Column(nullable = false, length = 255)
    var name: String = "",

    @Column(name = "local_name", length = 255)
    var localName: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Tournament = Tournament(
        id = id,
        providerId = providerId,
        sportId = sportId,
        externalId = externalId,
        name = name,
        localName = localName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(tournament: Tournament): TournamentEntity = TournamentEntity(
            id = tournament.id,
            providerId = tournament.providerId,
            sportId = tournament.sportId,
            externalId = tournament.externalId,
            name = tournament.name,
            localName = tournament.localName,
            createdAt = tournament.createdAt,
            updatedAt = tournament.updatedAt
        )
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = System.currentTimeMillis()
    }
}
