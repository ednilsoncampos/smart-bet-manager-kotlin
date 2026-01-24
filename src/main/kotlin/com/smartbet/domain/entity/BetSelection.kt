package com.smartbet.domain.entity

import com.smartbet.domain.enum.SelectionStatus
import java.math.BigDecimal

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

    /** ID do torneio (referência à tabela tournaments) */
    val tournamentId: Long? = null,

    /** Nome do torneio (derivado do relacionamento, usado em responses) */
    val tournamentName: String? = null,

    /** Tipo de mercado - ex: "Resultado Final", "Ambas Marcam" */
    val marketType: String? = null,
    
    /** Seleção escolhida - ex: "Flamengo", "Sim", "Over 2.5" */
    val selection: String,
    
    /** Odd no momento da aposta */
    val odd: BigDecimal,
    
    /** Status da seleção */
    val status: SelectionStatus = SelectionStatus.PENDING,
    
    /** Timestamp do evento (milissegundos desde epoch UTC) */
    val eventDate: Long? = null,
    
    /** Resultado do evento (se disponível) */
    val eventResult: String? = null,
    
    /** ID do esporte - ex: "FOOT", "BASK", "TENN" */
    val sportId: String? = null,
    
    /** Indica se é uma aposta combinada (Bet Builder) */
    val isBetBuilder: Boolean = false,
    
    /** Timestamp de criação (milissegundos desde epoch UTC) */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Timestamp de atualização (milissegundos desde epoch UTC) */
    val updatedAt: Long = System.currentTimeMillis()
)
