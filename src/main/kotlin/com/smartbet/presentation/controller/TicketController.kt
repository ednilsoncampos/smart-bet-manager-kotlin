package com.smartbet.presentation.controller

import com.smartbet.application.dto.*
import com.smartbet.application.usecase.TicketService
import com.smartbet.application.usecase.TicketRefreshService
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Gerenciamento de bilhetes de apostas")
class TicketController(
    private val ticketService: TicketService,
    private val ticketRefreshService: TicketRefreshService
) {
    
    @PostMapping("/import")
    @Operation(summary = "Importar bilhete via URL", description = "Importa um bilhete a partir de uma URL compartilhada")
    fun importFromUrl(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ImportTicketRequest
    ): ResponseEntity<TicketResponse> {
        val ticket = ticketService.importFromUrl(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket)
    }
    
    @PostMapping
    @Operation(summary = "Criar bilhete manual", description = "Cria um bilhete manualmente")
    fun createManual(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateManualTicketRequest
    ): ResponseEntity<TicketResponse> {
        val ticket = ticketService.createManual(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket)
    }
    
    @GetMapping
    @Operation(summary = "Listar bilhetes", description = "Lista bilhetes do usuário com filtros opcionais")
    fun list(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(required = false) status: TicketStatus?,
        @RequestParam(required = false) financialStatus: FinancialStatus?,
        @RequestParam(required = false) providerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<PagedResponse<TicketResponse>> {
        val request = ListTicketsRequest(
            status = status,
            financialStatus = financialStatus,
            providerId = providerId,
            page = page,
            pageSize = pageSize
        )
        val tickets = ticketService.listTickets(userId, request)
        return ResponseEntity.ok(tickets)
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar bilhete por ID", description = "Retorna os detalhes de um bilhete específico")
    fun getById(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long
    ): ResponseEntity<TicketResponse> {
        val ticket = ticketService.getById(userId, id)
        return ResponseEntity.ok(ticket)
    }
    
    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do bilhete", description = "Atualiza o status e/ou payout de um bilhete")
    fun updateStatus(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTicketStatusRequest
    ): ResponseEntity<TicketResponse> {
        val updatedRequest = request.copy(ticketId = id)
        val ticket = ticketService.updateStatus(userId, updatedRequest)
        return ResponseEntity.ok(ticket)
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar bilhete", description = "Remove um bilhete do sistema")
    fun delete(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        ticketService.delete(userId, id)
        return ResponseEntity.noContent().build()
    }
    
    @PostMapping("/refresh-open")
    @Operation(
        summary = "Atualizar bilhetes em aberto",
        description = "Dispara atualização assíncrona dos bilhetes em aberto do usuário. " +
            "Retorna imediatamente com status 202 Accepted enquanto o processamento ocorre em background. " +
            "Ideal para chamar no login do app."
    )
    fun refreshOpenTickets(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<RefreshOpenTicketsResponse> {
        val ticketsToRefresh = ticketService.countOpenTicketsToRefresh(userId)

        if (ticketsToRefresh == 0) {
            return ResponseEntity.ok(RefreshOpenTicketsResponse(
                message = "Nenhum bilhete em aberto para atualizar",
                ticketsToRefresh = 0,
                status = RefreshStatus.COMPLETED
            ))
        }

        // Dispara processamento assíncrono
        ticketRefreshService.refreshOpenTicketsAsync(userId)

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RefreshOpenTicketsResponse(
            message = "Atualizando $ticketsToRefresh bilhete(s) em background",
            ticketsToRefresh = ticketsToRefresh,
            status = RefreshStatus.PROCESSING
        ))
    }

    @PostMapping("/process-analytics")
    @Operation(
        summary = "Processar analytics de bilhetes liquidados",
        description = "Dispara processamento assíncrono de analytics para todos os bilhetes liquidados do usuário. " +
            "Gera e atualiza dados de performance (overall, por mês, por provider, por mercado, por torneio). " +
            "Retorna imediatamente com status 202 Accepted enquanto o processamento ocorre em background."
    )
    fun processSettledTicketsAnalytics(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<ProcessAnalyticsResponse> {
        val ticketsToProcess = ticketService.countSettledTicketsForAnalytics(userId)

        if (ticketsToProcess == 0) {
            return ResponseEntity.ok(ProcessAnalyticsResponse(
                message = "Nenhum bilhete liquidado para processar",
                ticketsToProcess = 0,
                status = AnalyticsProcessingStatus.COMPLETED
            ))
        }

        // Dispara processamento assíncrono
        ticketRefreshService.processSettledTicketsAnalyticsAsync(userId)

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ProcessAnalyticsResponse(
            message = "Processando analytics de $ticketsToProcess bilhete(s) em background",
            ticketsToProcess = ticketsToProcess,
            status = AnalyticsProcessingStatus.PROCESSING
        ))
    }
}
