package com.smartbet.domain.enum

/**
 * Status de uma seleção individual dentro de um bilhete.
 */
enum class SelectionStatus {
    /** Seleção ainda em andamento */
    PENDING,
    
    /** Seleção vencedora */
    WON,
    
    /** Seleção perdedora */
    LOST,
    
    /** Seleção anulada (odd ajustada para 1.0) */
    VOID,
    
    /** Seleção com cashout */
    CASHOUT
}
