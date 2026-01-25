package com.smartbet.domain.enum

/**
 * Status do bilhete de aposta conforme retornado pela casa de apostas.
 * Representa o estado atual do bilhete no sistema da casa.
 */
enum class TicketStatus {
    /** Aposta ainda em andamento, aguardando resultado */
    OPEN,

    /** Aposta vencedora (ganho total ou parcial, retorno > stake) */
    WIN,

    /** Aposta perdedora (perda total ou parcial, retorno <= stake) */
    LOST,

    /** Aposta anulada/cancelada (stake devolvido) */
    VOID,

    /** Aposta encerrada via cashout */
    CASHOUT
}
