package com.smartbet.domain.entity

import com.smartbet.domain.enum.TransactionType
import java.math.BigDecimal
import java.time.Instant

/**
 * Entidade de domínio representando uma transação na banca.
 */
data class BankrollTransaction(
    val id: Long? = null,
    
    /** ID da banca */
    val bankrollId: Long,
    
    /** ID do bilhete relacionado (se aplicável) */
    val ticketId: Long? = null,
    
    /** Tipo de transação */
    val type: TransactionType,
    
    /** Valor da transação (positivo = entrada, negativo = saída) */
    val amount: BigDecimal,
    
    /** Saldo após a transação */
    val balanceAfter: BigDecimal,
    
    /** Descrição da transação */
    val description: String? = null,
    
    val createdAt: Instant = Instant.now()
)
