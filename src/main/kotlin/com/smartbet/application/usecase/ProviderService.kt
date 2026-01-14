package com.smartbet.application.usecase

import com.smartbet.domain.entity.BettingProvider
import com.smartbet.infrastructure.persistence.entity.BettingProviderEntity
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import com.smartbet.infrastructure.provider.strategy.BettingProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class ProviderResponse(
    val id: Long,
    val slug: String,
    val name: String,
    val isActive: Boolean,
    val websiteUrl: String?,
    val logoUrl: String?,
    val isSupported: Boolean
)

data class CheckUrlResponse(
    val isSupported: Boolean,
    val providerSlug: String?,
    val providerName: String?,
    val ticketCode: String?
)

@Service
class ProviderService(
    private val providerRepository: BettingProviderRepository,
    private val providerFactory: BettingProviderFactory
) {
    private val logger = LoggerFactory.getLogger(ProviderService::class.java)
    
    /**
     * Lista todos os provedores cadastrados.
     */
    fun list(): List<ProviderResponse> {
        val providers = providerRepository.findAll()
        val supportedSlugs = providerFactory.getRegisteredProviders().toSet()
        
        return providers.map { entity ->
            ProviderResponse(
                id = entity.id!!,
                slug = entity.slug,
                name = entity.name,
                isActive = entity.isActive,
                websiteUrl = entity.websiteUrl,
                logoUrl = entity.logoUrl,
                isSupported = entity.slug in supportedSlugs
            )
        }
    }
    
    /**
     * Lista apenas os provedores ativos.
     */
    fun listActive(): List<ProviderResponse> {
        val providers = providerRepository.findByIsActiveTrue()
        val supportedSlugs = providerFactory.getRegisteredProviders().toSet()
        
        return providers.map { entity ->
            ProviderResponse(
                id = entity.id!!,
                slug = entity.slug,
                name = entity.name,
                isActive = entity.isActive,
                websiteUrl = entity.websiteUrl,
                logoUrl = entity.logoUrl,
                isSupported = entity.slug in supportedSlugs
            )
        }
    }
    
    /**
     * Busca um provedor por ID.
     */
    fun getById(providerId: Long): ProviderResponse {
        val entity = providerRepository.findById(providerId)
            .orElseThrow { IllegalArgumentException("Provedor não encontrado: $providerId") }
        
        val supportedSlugs = providerFactory.getRegisteredProviders().toSet()
        
        return ProviderResponse(
            id = entity.id!!,
            slug = entity.slug,
            name = entity.name,
            isActive = entity.isActive,
            websiteUrl = entity.websiteUrl,
            logoUrl = entity.logoUrl,
            isSupported = entity.slug in supportedSlugs
        )
    }
    
    /**
     * Busca um provedor por slug.
     */
    fun getBySlug(slug: String): ProviderResponse? {
        val entity = providerRepository.findBySlug(slug) ?: return null
        val supportedSlugs = providerFactory.getRegisteredProviders().toSet()
        
        return ProviderResponse(
            id = entity.id!!,
            slug = entity.slug,
            name = entity.name,
            isActive = entity.isActive,
            websiteUrl = entity.websiteUrl,
            logoUrl = entity.logoUrl,
            isSupported = entity.slug in supportedSlugs
        )
    }
    
    /**
     * Verifica se uma URL é suportada e extrai informações.
     */
    fun checkUrl(url: String): CheckUrlResponse {
        val strategy = providerFactory.findStrategyForUrl(url)
        
        return if (strategy != null) {
            val ticketCode = strategy.extractTicketCode(url)
            CheckUrlResponse(
                isSupported = true,
                providerSlug = strategy.slug,
                providerName = strategy.name,
                ticketCode = ticketCode
            )
        } else {
            val providerName = providerFactory.extractProviderNameFromUrl(url)
            CheckUrlResponse(
                isSupported = false,
                providerSlug = null,
                providerName = providerName,
                ticketCode = null
            )
        }
    }
    
    /**
     * Retorna lista de provedores suportados (com parser implementado).
     */
    fun getSupportedProviders(): List<String> {
        return providerFactory.getRegisteredProviders()
    }
}
