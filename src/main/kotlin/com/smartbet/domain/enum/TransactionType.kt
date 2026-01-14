package com.smartbet.domain.enum

/**
 * Tipo de transação na banca.
 */
enum class TransactionType {
    /** Depósito na banca */
    DEPOSIT,
    
    /** Saque da banca */
    WITHDRAWAL,
    
    /** Aposta realizada (débito) */
    BET_PLACED,
    
    /** Retorno de aposta (crédito) */
    BET_RETURN,
    
    /** Bônus recebido */
    BONUS,
    
    /** Ajuste manual */
    ADJUSTMENT
}
