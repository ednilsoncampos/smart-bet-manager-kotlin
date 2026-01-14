package com.smartbet.presentation.controller

import com.smartbet.application.dto.*
import com.smartbet.application.usecase.BankrollService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bankrolls")
@Tag(name = "Bankrolls", description = "Gerenciamento de bancas de apostas")
class BankrollController(
    private val bankrollService: BankrollService
) {
    
    @PostMapping
    @Operation(summary = "Criar banca", description = "Cria uma nova banca de apostas")
    fun create(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: CreateBankrollRequest
    ): ResponseEntity<BankrollResponse> {
        val bankroll = bankrollService.create(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(bankroll)
    }
    
    @GetMapping
    @Operation(summary = "Listar bancas", description = "Lista todas as bancas ativas do usuário")
    fun list(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<List<BankrollResponse>> {
        val bankrolls = bankrollService.list(userId)
        return ResponseEntity.ok(bankrolls)
    }
    
    @GetMapping("/summary")
    @Operation(summary = "Resumo das bancas", description = "Retorna o resumo consolidado de todas as bancas")
    fun getSummary(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<BankrollSummaryResponse> {
        val summary = bankrollService.getSummary(userId)
        return ResponseEntity.ok(summary)
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar banca por ID", description = "Retorna os detalhes de uma banca específica")
    fun getById(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long
    ): ResponseEntity<BankrollResponse> {
        val bankroll = bankrollService.getById(userId, id)
        return ResponseEntity.ok(bankroll)
    }
    
    @PostMapping("/{id}/transactions")
    @Operation(summary = "Registrar transação", description = "Registra uma transação na banca (depósito, saque, etc)")
    fun recordTransaction(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: RecordTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val updatedRequest = request.copy(bankrollId = id)
        val transaction = bankrollService.recordTransaction(userId, updatedRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction)
    }
    
    @GetMapping("/{id}/transactions")
    @Operation(summary = "Listar transações", description = "Lista as transações de uma banca")
    fun listTransactions(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<PagedResponse<TransactionResponse>> {
        val transactions = bankrollService.listTransactions(userId, id, page, pageSize)
        return ResponseEntity.ok(transactions)
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar banca", description = "Desativa uma banca (soft delete)")
    fun deactivate(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        bankrollService.deactivate(userId, id)
        return ResponseEntity.noContent().build()
    }
}
