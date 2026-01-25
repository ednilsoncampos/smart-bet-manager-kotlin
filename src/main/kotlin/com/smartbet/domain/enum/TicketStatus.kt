package com.smartbet.domain.enum

/**
 * Status do bilhete de aposta conforme retornado pela casa de apostas.
 * Representa o estado atual do bilhete no sistema da casa.
 */
enum class TicketStatus {
    /** Aposta ainda em andamento, aguardando resultado */
    OPEN,
    
    /** Aposta vencedora (ganho total, todas as seleções corretas) */
    WIN,
    
    /** Aposta com ganho parcial (algumas seleções corretas, lucro positivo) */
    PARTIAL_WIN,
    
    /** Aposta perdedora (perda total) */
    LOST,
    
    /** Aposta com perda parcial (algumas seleções corretas, mas prejuízo) */
    PARTIAL_LOSS,
    
    /** Aposta anulada/cancelada (stake devolvido) */
    VOID,
    
    /** Aposta encerrada via cashout */
    CASHOUT
}
