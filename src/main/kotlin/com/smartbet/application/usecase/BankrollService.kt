package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.enum.TransactionType
import com.smartbet.infrastructure.persistence.entity.BankrollEntity
import com.smartbet.infrastructure.persistence.entity.BankrollTransactionEntity
import com.smartbet.infrastructure.persistence.repository.BankrollRepository
import com.smartbet.infrastructure.persistence.repository.BankrollTransactionRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class BankrollService(
    private val bankrollRepository: BankrollRepository,
    private val transactionRepository: BankrollTransactionRepository,
    private val providerRepository: BettingProviderRepository
) {
    private val logger = LoggerFactory.getLogger(BankrollService::class.java)
    
    /**
     * Cria uma nova banca.
     */
    @Transactional
    fun create(userId: Long, request: CreateBankrollRequest): BankrollResponse {
        logger.info("Creating bankroll for user: {}", userId)
        
        val providerName = request.providerId?.let { providerId ->
            providerRepository.findById(providerId)
                .orElseThrow { IllegalArgumentException("Provider não encontrado: $providerId") }
                .name
        }
        
        val bankrollEntity = BankrollEntity(
            userId = userId,
            providerId = request.providerId,
            name = request.name,
            currency = request.currency,
            currentBalance = request.initialDeposit ?: BigDecimal.ZERO,
            totalDeposited = request.initialDeposit ?: BigDecimal.ZERO
        )
        
        val savedBankroll = bankrollRepository.save(bankrollEntity)
        
        // Se houver depósito inicial, registra a transação
        request.initialDeposit?.let { deposit ->
            if (deposit > BigDecimal.ZERO) {
                val transaction = BankrollTransactionEntity(
                    bankrollId = savedBankroll.id!!,
                    type = TransactionType.DEPOSIT,
                    amount = deposit,
                    balanceAfter = deposit,
                    description = "Depósito inicial"
                )
                transactionRepository.save(transaction)
            }
        }
        
        logger.info("Bankroll created: {}", savedBankroll.id)
        
        return BankrollResponse.fromDomain(savedBankroll.toDomain(), providerName)
    }
    
    /**
     * Lista todas as bancas do usuário.
     */
    fun list(userId: Long): List<BankrollResponse> {
        val bankrolls = bankrollRepository.findByUserIdAndIsActiveTrue(userId)
        val providerMap = providerRepository.findAll().associateBy { it.id }
        
        return bankrolls.map { entity ->
            BankrollResponse.fromDomain(
                entity.toDomain(),
                entity.providerId?.let { providerMap[it]?.name }
            )
        }
    }
    
    /**
     * Busca uma banca por ID.
     */
    fun getById(userId: Long, bankrollId: Long): BankrollResponse {
        val bankroll = bankrollRepository.findById(bankrollId)
            .orElseThrow { IllegalArgumentException("Banca não encontrada: $bankrollId") }
        
        if (bankroll.userId != userId) {
            throw IllegalAccessException("Acesso negado à banca: $bankrollId")
        }
        
        val providerName = bankroll.providerId?.let { providerId ->
            providerRepository.findById(providerId).orElse(null)?.name
        }
        
        return BankrollResponse.fromDomain(bankroll.toDomain(), providerName)
    }
    
    /**
     * Registra uma transação na banca.
     */
    @Transactional
    fun recordTransaction(userId: Long, request: RecordTransactionRequest): TransactionResponse {
        logger.info("Recording transaction for bankroll: {}", request.bankrollId)
        
        val bankroll = bankrollRepository.findById(request.bankrollId)
            .orElseThrow { IllegalArgumentException("Banca não encontrada: ${request.bankrollId}") }
        
        if (bankroll.userId != userId) {
            throw IllegalAccessException("Acesso negado à banca: ${request.bankrollId}")
        }
        
        // Calcula o novo saldo
        val amount = when (request.type) {
            TransactionType.DEPOSIT, TransactionType.BET_RETURN, TransactionType.BONUS -> request.amount
            TransactionType.WITHDRAWAL, TransactionType.BET_PLACED -> request.amount.negate()
            TransactionType.ADJUSTMENT -> request.amount
        }
        
        val newBalance = bankroll.currentBalance + amount
        
        // Atualiza a banca
        bankroll.currentBalance = newBalance
        when (request.type) {
            TransactionType.DEPOSIT -> bankroll.totalDeposited = bankroll.totalDeposited + request.amount
            TransactionType.WITHDRAWAL -> bankroll.totalWithdrawn = bankroll.totalWithdrawn + request.amount
            TransactionType.BET_PLACED -> bankroll.totalStaked = bankroll.totalStaked + request.amount
            TransactionType.BET_RETURN -> bankroll.totalReturns = bankroll.totalReturns + request.amount
            else -> {}
        }
        
        bankrollRepository.save(bankroll)
        
        // Cria a transação
        val transaction = BankrollTransactionEntity(
            bankrollId = bankroll.id!!,
            type = request.type,
            amount = request.amount,
            balanceAfter = newBalance,
            description = request.description
        )
        
        val savedTransaction = transactionRepository.save(transaction)
        
        logger.info("Transaction recorded: {} -> {}", savedTransaction.id, newBalance)
        
        return TransactionResponse.fromDomain(savedTransaction.toDomain())
    }
    
    /**
     * Lista transações de uma banca.
     */
    fun listTransactions(
        userId: Long, 
        bankrollId: Long, 
        page: Int = 0, 
        pageSize: Int = 20
    ): PagedResponse<TransactionResponse> {
        val bankroll = bankrollRepository.findById(bankrollId)
            .orElseThrow { IllegalArgumentException("Banca não encontrada: $bankrollId") }
        
        if (bankroll.userId != userId) {
            throw IllegalAccessException("Acesso negado à banca: $bankrollId")
        }
        
        val pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        val transactionPage = transactionRepository.findByBankrollIdOrderByCreatedAtDesc(bankrollId, pageable)
        
        return PagedResponse(
            content = transactionPage.content.map { TransactionResponse.fromDomain(it.toDomain()) },
            page = transactionPage.number,
            pageSize = transactionPage.size,
            totalElements = transactionPage.totalElements,
            totalPages = transactionPage.totalPages,
            hasNext = transactionPage.hasNext(),
            hasPrevious = transactionPage.hasPrevious()
        )
    }
    
    /**
     * Retorna o resumo consolidado de todas as bancas do usuário.
     */
    fun getSummary(userId: Long): BankrollSummaryResponse {
        val bankrolls = bankrollRepository.findByUserIdAndIsActiveTrue(userId)
        
        val totalBalance = bankrolls.sumOf { it.currentBalance }
        val totalDeposited = bankrolls.sumOf { it.totalDeposited }
        val totalWithdrawn = bankrolls.sumOf { it.totalWithdrawn }
        val totalStaked = bankrolls.sumOf { it.totalStaked }
        val totalReturns = bankrolls.sumOf { it.totalReturns }
        val overallProfitLoss = totalReturns - totalStaked
        
        val overallRoi = if (totalStaked > BigDecimal.ZERO) {
            overallProfitLoss
                .divide(totalStaked, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        return BankrollSummaryResponse(
            totalBalance = totalBalance,
            totalDeposited = totalDeposited,
            totalWithdrawn = totalWithdrawn,
            totalStaked = totalStaked,
            totalReturns = totalReturns,
            overallProfitLoss = overallProfitLoss,
            overallRoi = overallRoi,
            bankrollCount = bankrolls.size
        )
    }
    
    /**
     * Desativa uma banca (soft delete).
     */
    @Transactional
    fun deactivate(userId: Long, bankrollId: Long) {
        val bankroll = bankrollRepository.findById(bankrollId)
            .orElseThrow { IllegalArgumentException("Banca não encontrada: $bankrollId") }
        
        if (bankroll.userId != userId) {
            throw IllegalAccessException("Acesso negado à banca: $bankrollId")
        }
        
        bankroll.isActive = false
        bankrollRepository.save(bankroll)
        
        logger.info("Bankroll deactivated: {}", bankrollId)
    }
}

private fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
