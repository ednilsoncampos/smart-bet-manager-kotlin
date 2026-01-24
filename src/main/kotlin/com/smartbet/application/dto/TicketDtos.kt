package com.smartbet.application.dto

import com.smartbet.domain.entity.BetSelection
import com.smartbet.domain.entity.BetTicket
import com.smartbet.domain.enum.*
import java.math.BigDecimal

// ============================================
// Request DTOs
// ============================================

data class ImportTicketRequest(
    val url: String,
    val bankrollId: Long? = null
)

data class CreateManualTicketRequest(
    val providerId: Long,
    val bankrollId: Long? = null,
    val betType: BetType = BetType.SINGLE,
    val stake: BigDecimal,
    val totalOdd: BigDecimal,
    val potentialPayout: BigDecimal? = null,
    val systemDescription: String? = null,
    val placedAt: Long? = null,
    val selections: List<CreateSelectionRequest>
)

data class CreateSelectionRequest(
    val eventName: String,
    val tournamentId: Long? = null,
    val marketType: String? = null,
    val selection: String,
    val odd: BigDecimal,
    val eventDate: Long? = null
)

data class UpdateTicketStatusRequest(
    val ticketId: Long,
    val actualPayout: BigDecimal? = null,
    val ticketStatus: TicketStatus? = null
)

data class ListTicketsRequest(
    val status: TicketStatus? = null,
    val financialStatus: FinancialStatus? = null,
    val providerId: Long? = null,
    val page: Int = 0,
    val pageSize: Int = 20
)

// ============================================
// Response DTOs
// ============================================

data class TicketResponse(
    val id: Long,
    val providerId: Long,
    val providerName: String?,
    val bankrollId: Long?,
    val externalTicketId: String?,
    val sourceUrl: String?,
    val betType: BetType,
    val betSide: BetSide,
    val stake: BigDecimal,
    val totalOdd: BigDecimal,
    val potentialPayout: BigDecimal?,
    val actualPayout: BigDecimal?,
    val ticketStatus: TicketStatus,
    val financialStatus: FinancialStatus,
    val profitLoss: BigDecimal,
    val roi: BigDecimal,
    val systemDescription: String?,
    val placedAt: Long?,
    val settledAt: Long?,
    val selections: List<SelectionResponse>,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromDomain(ticket: BetTicket, providerName: String? = null): TicketResponse {
            return TicketResponse(
                id = ticket.id!!,
                providerId = ticket.providerId,
                providerName = providerName,
                bankrollId = ticket.bankrollId,
                externalTicketId = ticket.externalTicketId,
                sourceUrl = ticket.sourceUrl,
                betType = ticket.betType,
                betSide = ticket.betSide,
                stake = ticket.stake,
                totalOdd = ticket.totalOdd,
                potentialPayout = ticket.potentialPayout,
                actualPayout = ticket.actualPayout,
                ticketStatus = ticket.ticketStatus,
                financialStatus = ticket.financialStatus,
                profitLoss = ticket.profitLoss,
                roi = ticket.roi,
                systemDescription = ticket.systemDescription,
                placedAt = ticket.placedAt,
                settledAt = ticket.settledAt,
                selections = ticket.selections.map { SelectionResponse.fromDomain(it) },
                createdAt = ticket.createdAt,
                updatedAt = ticket.updatedAt
            )
        }
    }
}

data class SelectionResponse(
    val id: Long?,
    val eventName: String,
    val tournamentName: String?,
    val marketType: String?,
    val selection: String,
    val odd: BigDecimal,
    val status: SelectionStatus,
    val eventDate: Long?,
    val eventResult: String?
) {
    companion object {
        fun fromDomain(selection: BetSelection): SelectionResponse {
            return SelectionResponse(
                id = selection.id,
                eventName = selection.eventName,
                tournamentName = selection.tournamentName,
                marketType = selection.marketType,
                selection = selection.selection,
                odd = selection.odd,
                status = selection.status,
                eventDate = selection.eventDate,
                eventResult = selection.eventResult
            )
        }
    }
}

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

// ============================================
// Refresh Open Tickets DTOs
// ============================================

/**
 * Resposta do endpoint de refresh de bilhetes em aberto.
 * Retornado imediatamente (202 Accepted) enquanto o processamento ocorre em background.
 */
data class RefreshOpenTicketsResponse(
    val message: String,
    val ticketsToRefresh: Int,
    val status: RefreshStatus = RefreshStatus.PROCESSING
)

/**
 * Resultado do processamento de refresh de bilhetes.
 * Usado internamente e nos logs.
 */
data class RefreshResult(
    val totalProcessed: Int,
    val updated: Int,
    val unchanged: Int,
    val errors: Int,
    val errorDetails: List<RefreshError> = emptyList()
)

/**
 * Detalhes de erro durante o refresh de um bilhete.
 */
data class RefreshError(
    val ticketId: Long,
    val externalTicketId: String?,
    val errorMessage: String
)

/**
 * Status do processamento de refresh.
 */
enum class RefreshStatus {
    PROCESSING,
    COMPLETED,
    PARTIAL_ERROR,
    FAILED
}
