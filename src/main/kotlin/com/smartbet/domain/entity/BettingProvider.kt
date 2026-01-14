package com.smartbet.domain.entity

import java.time.Instant

/**
 * Entidade de domínio representando uma casa de apostas.
 * 
 * Contém informações sobre a casa de apostas e como acessar sua API.
 */
data class BettingProvider(
    val id: Long? = null,
    
    /** Identificador único (slug) da casa - ex: "superbet", "betano" */
    val slug: String,
    
    /** Nome de exibição da casa */
    val name: String,
    
    /** Se a integração está ativa */
    val isActive: Boolean = true,
    
    /** Template da URL da API para buscar bilhetes */
    val apiUrlTemplate: String? = null,
    
    /** URL base do site da casa */
    val websiteUrl: String? = null,
    
    /** Logo da casa (URL) */
    val logoUrl: String? = null,
    
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    companion object {
        const val SUPERBET = "superbet"
        const val BETANO = "betano"
        const val BETFAIR = "betfair"
        const val BET365 = "bet365"
    }
}
