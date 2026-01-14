package com.smartbet.domain.enum

/**
 * Status financeiro real da aposta, calculado com base no retorno efetivo.
 * 
 * Este enum representa o resultado financeiro REAL da aposta, não apenas
 * se ela foi "ganha" ou "perdida". É especialmente importante para:
 * - Apostas de sistema (múltiplas combinações)
 * - Cashout parcial
 * - Apostas com seleções anuladas
 * 
 * A ordem dos valores representa a progressão de melhor para pior resultado.
 */
enum class FinancialStatus {
    /** Aguardando resultado - aposta ainda em aberto */
    PENDING,
    
    /** Ganho total - retorno >= potencial máximo */
    FULL_WIN,
    
    /** Ganho parcial - retorno > stake, mas < potencial */
    PARTIAL_WIN,
    
    /** Empate - retorno = stake (sem lucro nem prejuízo) */
    BREAK_EVEN,
    
    /** Perda parcial - 0 < retorno < stake */
    PARTIAL_LOSS,
    
    /** Perda total - retorno = 0 */
    TOTAL_LOSS
}
