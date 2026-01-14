package com.smartbet.domain.entity

import com.smartbet.domain.enum.SelectionStatus
import java.math.BigDecimal
import java.time.Instant

/**
 * Entidade de domínio representando uma seleção individual dentro de um bilhete.
 * 
 * Cada seleção representa um evento/mercado específico apostado.
 */
data class BetSelection(
    val id: Long? = null,
    
    /** ID do bilhete pai */
    val ticketId: Long,
    
    /** ID externo da seleção na casa de apostas */
    val externalSelectionId: String? = null,
    
    /** Nome do evento - ex: "Flamengo x Palmeiras" */
    val eventName: String,
    
    /** Nome do campeonato/competição */
    val tournamentName: String? = null,
    
    /** Tipo de mercado - ex: "Resultado Final", "Ambas Marcam" */
    val marketType: String? = null,
    
    /** Seleção escolhida - ex: "Flamengo", "Sim", "Over 2.5" */
    val selection: String,
    
    /** Odd no momento da aposta */
    val odd: BigDecimal,
    
    /** Status da seleção */
    val status: SelectionStatus = SelectionStatus.PENDING,
    
    /** Data/hora do evento */
    val eventDate: Instant? = null,
    
    /** Resultado do evento (se disponível) */
    val eventResult: String? = null,
    
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
