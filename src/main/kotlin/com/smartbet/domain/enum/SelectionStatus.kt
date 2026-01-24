package com.smartbet.domain.enum

/**
 * Status de uma seleção individual dentro de um bilhete.
 */
enum class SelectionStatus {
    /** Seleção ainda em andamento */
    PENDING,
    
    /** Seleção vencedora (acerto total) */
    WON,
    
    /** Seleção meio ganha (ex: handicap asiático) */
    HALF_WON,
    
    /** Seleção perdedora (erro total) */
    LOST,
    
    /** Seleção meio perdida (ex: handicap asiático) */
    HALF_LOST,
    
    /** Seleção anulada (odd ajustada para 1.0) */
    VOID,
    
    /** Seleção com cashout */
    CASHOUT
}
