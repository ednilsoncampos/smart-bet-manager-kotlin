package com.smartbet.domain.event

import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.enum.TicketStatus
import java.math.BigDecimal

/**
 * Evento de domínio publicado quando um ticket é liquidado.
 *
 * Este evento dispara a atualização assíncrona das tabelas de analytics.
 * Contém todos os dados necessários para calcular os deltas nas agregações.
 */
data class TicketSettledEvent(
    val ticketId: Long,
    val userId: Long,
    val providerId: Long,

    // Dados financeiros
    val stake: BigDecimal,
    val totalOdd: BigDecimal,
    val actualPayout: BigDecimal,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,

    // Status
    val ticketStatus: TicketStatus,
    val financialStatus: FinancialStatus,

    // Timestamps
    val settledAt: Long,

    // Dados das seleções para analytics detalhados
    val selections: List<SelectionData>
) {
    /**
     * Dados de uma seleção para analytics.
     * Contém informações necessárias para agregações por mercado e torneio.
     */
    data class SelectionData(
        val marketType: String,
        val status: SelectionStatus,
        val tournamentId: Long?,
        val eventDate: Long?
    )
}
