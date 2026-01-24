package com.smartbet.infrastructure.provider.strategy

import com.smartbet.domain.entity.BetSelection
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import java.math.BigDecimal

/**
 * Interface Strategy para parsers de casas de apostas.
 * 
 * Cada casa de apostas implementa esta interface para:
 * - Identificar se uma URL pertence à casa
 * - Extrair o código do bilhete da URL
 * - Construir a URL da API
 * - Parsear a resposta da API
 */
interface BettingProviderStrategy {
    
    /** Slug único da casa de apostas */
    val slug: String
    
    /** Nome de exibição da casa */
    val name: String
    
    /** Padrões de URL que esta strategy pode processar */
    val urlPatterns: List<Regex>
    
    /** Template padrão da URL da API */
    val defaultApiTemplate: String
    
    /**
     * Verifica se esta strategy pode processar a URL fornecida.
     */
    fun canHandle(url: String): Boolean {
        return urlPatterns.any { it.containsMatchIn(url) }
    }
    
    /**
     * Extrai o código do bilhete da URL.
     * 
     * @param url URL do bilhete compartilhado
     * @return Código do bilhete ou null se não encontrado
     */
    fun extractTicketCode(url: String): String?
    
    /**
     * Constrói a URL da API para buscar o bilhete.
     * 
     * @param ticketCode Código do bilhete
     * @param apiTemplate Template da URL (opcional, usa default se null)
     * @return URL completa da API
     */
    fun buildApiUrl(ticketCode: String, apiTemplate: String? = null): String {
        val template = apiTemplate ?: defaultApiTemplate
        return template.replace("{CODE}", ticketCode)
    }
    
    /**
     * Parseia a resposta da API e retorna os dados do bilhete.
     * 
     * @param responseBody Corpo da resposta da API (JSON)
     * @return Dados parseados do bilhete
     */
    fun parseResponse(responseBody: String): ParsedTicketData
}

/**
 * Dados parseados de um bilhete de aposta.
 * Timestamps são em milissegundos (epoch).
 */
data class ParsedTicketData(
    val externalTicketId: String,
    val betType: BetType,
    val stake: BigDecimal,
    val totalOdd: BigDecimal,
    val potentialPayout: BigDecimal?,
    val actualPayout: BigDecimal?,
    val ticketStatus: TicketStatus,
    val systemDescription: String? = null,
    val placedAt: Long? = null,
    val settledAt: Long? = null,
    val isCashedOut: Boolean = false,
    val selections: List<ParsedSelectionData>,
    /** Componentes das seleções (para Bet Builder) - mapa de selectionId para lista de componentes */
    val selectionComponents: Map<String, List<ParsedSelectionComponentData>> = emptyMap()
)

/**
 * Dados parseados de uma seleção.
 * Timestamps são em milissegundos (epoch).
 */
data class ParsedSelectionData(
    val externalSelectionId: String?,
    val eventName: String,
    /** ID externo do torneio na API do provider (será resolvido para Tournament.id) */
    val externalTournamentId: Int?,
    val marketType: String?,
    val selection: String,
    val odd: BigDecimal,
    val status: com.smartbet.domain.enum.SelectionStatus,
    val eventDate: Long?,
    val eventResult: String?,
    /** ID do esporte - ex: "5" (Superbet), "FOOT" (Betano) */
    val sportId: String? = null,
    /** Indica se é uma aposta combinada (Bet Builder) */
    val isBetBuilder: Boolean = false
)

/**
 * Dados parseados de um componente de seleção (para Bet Builder).
 */
data class ParsedSelectionComponentData(
    val marketId: String?,
    val marketName: String,
    val selectionName: String,
    val status: com.smartbet.domain.enum.SelectionStatus
)
