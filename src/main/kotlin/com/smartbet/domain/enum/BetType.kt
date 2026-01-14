package com.smartbet.domain.enum

/**
 * Tipo de aposta.
 */
enum class BetType {
    /** Aposta simples - uma única seleção */
    SINGLE,
    
    /** Aposta múltipla/acumulada - todas seleções devem acertar */
    MULTIPLE,
    
    /** Aposta de sistema - combinações parciais */
    SYSTEM
}
