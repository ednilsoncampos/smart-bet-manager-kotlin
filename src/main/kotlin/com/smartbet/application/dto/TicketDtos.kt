package com.smartbet.application.dto

import com.smartbet.common.requireId
import com.smartbet.domain.entity.BetSelection
import com.smartbet.domain.entity.BetSelectionComponent
import com.smartbet.domain.entity.BetTicket
import com.smartbet.domain.enum.*
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal

// ============================================
// Request DTOs
// ============================================

data class ImportTicketRequest(
    @field:NotBlank(message = "URL é obrigatória")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "URL deve ser válida (começar com http:// ou https://)"
    )
    val url: String,

    @field:Positive(message = "Bankroll ID deve ser um número positivo")
    val bankrollId: Long? = null
)

data class CreateManualTicketRequest(
    @field:Positive(message = "Provider ID é obrigatório e deve ser positivo")
    val providerId: Long,

    @field:Positive(message = "Bankroll ID deve ser um número positivo")
    val bankrollId: Long? = null,

    val betType: BetType = BetType.SINGLE,

    @field:DecimalMin(value = "0.01", message = "Stake deve ser maior que zero")
    @field:Digits(integer = 10, fraction = 2, message = "Stake deve ter no máximo 2 casas decimais")
    val stake: BigDecimal,

    @field:DecimalMin(value = "1.01", message = "Odd total deve ser maior ou igual a 1.01")
    @field:Digits(integer = 10, fraction = 2, message = "Odd total deve ter no máximo 2 casas decimais")
    val totalOdd: BigDecimal,

    @field:Positive(message = "Payout potencial deve ser positivo")
    val potentialPayout: BigDecimal? = null,

    @field:Size(max = 100, message = "Descrição do sistema deve ter no máximo 100 caracteres")
    val systemDescription: String? = null,

    val placedAt: Long? = null,

    @field:NotEmpty(message = "Deve ter pelo menos uma seleção")
    @field:Valid
    val selections: List<CreateSelectionRequest>
)

data class CreateSelectionRequest(
    @field:NotBlank(message = "Nome do evento é obrigatório")
    @field:Size(max = 200, message = "Nome do evento deve ter no máximo 200 caracteres")
    val eventName: String,

    @field:Positive(message = "Tournament ID deve ser um número positivo")
    val tournamentId: Long? = null,

    @field:Size(max = 100, message = "Tipo de mercado deve ter no máximo 100 caracteres")
    val marketType: String? = null,

    @field:NotBlank(message = "Seleção é obrigatória")
    @field:Size(max = 200, message = "Seleção deve ter no máximo 200 caracteres")
    val selection: String,

    @field:DecimalMin(value = "1.01", message = "Odd deve ser maior ou igual a 1.01")
    @field:Digits(integer = 10, fraction = 2, message = "Odd deve ter no máximo 2 casas decimais")
    val odd: BigDecimal,

    val eventDate: Long? = null
)

data class UpdateTicketStatusRequest(
    @field:Positive(message = "Ticket ID é obrigatório e deve ser positivo")
    val ticketId: Long,

    @field:PositiveOrZero(message = "Payout real deve ser zero ou positivo")
    val actualPayout: BigDecimal? = null,

    val ticketStatus: TicketStatus? = null
)

data class ListTicketsRequest(
    val status: TicketStatus? = null,
    val financialStatus: FinancialStatus? = null,

    @field:Positive(message = "Provider ID deve ser um número positivo")
    val providerId: Long? = null,

    @field:Min(value = 0, message = "Página deve ser zero ou maior")
    val page: Int = 0,

    @field:Min(value = 1, message = "Tamanho da página deve ser no mínimo 1")
    @field:Max(value = 100, message = "Tamanho da página deve ser no máximo 100")
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
    /** Indica se o bilhete foi encerrado via cashout */
    val isCashedOut: Boolean,
    val selections: List<SelectionResponse>,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromDomain(
            ticket: BetTicket,
            providerName: String? = null,
            selectionComponentsMap: Map<Long, List<BetSelectionComponent>> = emptyMap()
        ): TicketResponse {
            return TicketResponse(
                id = ticket.id.requireId("BetTicket"),
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
                isCashedOut = ticket.isCashedOut,
                selections = ticket.selections.map { selection ->
                    val components = selectionComponentsMap[selection.id] ?: emptyList()
                    SelectionResponse.fromDomain(selection, components)
                },
                createdAt = ticket.createdAt,
                updatedAt = ticket.updatedAt
            )
        }
    }
}

data class BetBuilderComponentDetail(
    val marketName: String,
    val selectionName: String,
    val status: SelectionStatus
)

data class SelectionResponse(
    val id: Long?,
    val eventName: String,
    val tournamentName: String?,
    val marketType: String?,
    val odd: BigDecimal,
    val status: SelectionStatus,
    val eventDate: Long?,
    val eventResult: String?,
    val components: List<BetBuilderComponentDetail>
) {
    companion object {
        fun fromDomain(
            selection: BetSelection,
            components: List<BetSelectionComponent> = emptyList()
        ): SelectionResponse {
            // Se não houver componentes, cria um único componente a partir dos dados da seleção
            val componentList = if (components.isEmpty()) {
                listOf(
                    BetBuilderComponentDetail(
                        marketName = selection.marketType ?: "Desconhecido",
                        selectionName = selection.selection,
                        status = selection.status
                    )
                )
            } else {
                components.map { component ->
                    BetBuilderComponentDetail(
                        marketName = component.marketName,
                        selectionName = component.selectionName,
                        status = component.status
                    )
                }
            }

            return SelectionResponse(
                id = selection.id,
                eventName = selection.eventName,
                tournamentName = selection.tournamentName,
                marketType = selection.marketType,
                odd = selection.odd,
                status = selection.status,
                eventDate = selection.eventDate,
                eventResult = selection.eventResult,
                components = componentList
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

// ============================================
// Analytics Processing DTOs
// ============================================

/**
 * Resposta para a requisição de processamento de analytics de bilhetes liquidados.
 */
data class ProcessAnalyticsResponse(
    val message: String,
    val ticketsToProcess: Int,
    val status: AnalyticsProcessingStatus = AnalyticsProcessingStatus.PROCESSING
)

/**
 * Resultado do processamento de analytics.
 */
data class AnalyticsProcessingResult(
    val totalProcessed: Int,
    val successful: Int,
    val errors: Int,
    val errorDetails: List<AnalyticsProcessingError> = emptyList()
)

/**
 * Erro no processamento de analytics de um bilhete.
 */
data class AnalyticsProcessingError(
    val ticketId: Long,
    val externalTicketId: String?,
    val errorMessage: String
)

/**
 * Status do processamento de analytics.
 */
enum class AnalyticsProcessingStatus {
    PROCESSING,
    COMPLETED,
    PARTIAL_ERROR,
    FAILED
}
