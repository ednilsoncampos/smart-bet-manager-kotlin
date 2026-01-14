package com.smartbet.domain.enum

/**
 * Status do bilhete de aposta conforme retornado pela casa de apostas.
 * Representa o estado atual do bilhete no sistema da casa.
 */
enum class TicketStatus {
    /** Aposta ainda em andamento, aguardando resultado */
    OPEN,
    
    /** Aposta vencedora (pode ser ganho total ou parcial) */
    WON,
    
    /** Aposta perdedora */
    LOST,
    
    /** Aposta anulada/cancelada (stake devolvido) */
    VOID,
    
    /** Aposta encerrada via cashout */
    CASHOUT
}
