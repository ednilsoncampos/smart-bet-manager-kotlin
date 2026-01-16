package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.BettingProvider
import jakarta.persistence.*

@Entity
@Table(name = "betting_providers")
class BettingProviderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, unique = true, length = 50)
    var slug: String = "",
    
    @Column(nullable = false, length = 100)
    var name: String = "",
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    
    @Column(name = "api_url_template", length = 500)
    var apiUrlTemplate: String? = null,
    
    @Column(name = "website_url")
    var websiteUrl: String? = null,
    
    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BettingProvider = BettingProvider(
        id = id,
        slug = slug,
        name = name,
        isActive = isActive,
        apiUrlTemplate = apiUrlTemplate,
        websiteUrl = websiteUrl,
        logoUrl = logoUrl,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    companion object {
        fun fromDomain(provider: BettingProvider): BettingProviderEntity = BettingProviderEntity(
            id = provider.id,
            slug = provider.slug,
            name = provider.name,
            isActive = provider.isActive,
            apiUrlTemplate = provider.apiUrlTemplate,
            websiteUrl = provider.websiteUrl,
            logoUrl = provider.logoUrl,
            createdAt = provider.createdAt,
            updatedAt = provider.updatedAt
        )
    }
    
    @PreUpdate
    fun preUpdate() {
        updatedAt = System.currentTimeMillis()
    }
}
