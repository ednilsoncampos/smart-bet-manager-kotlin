package com.smartbet.presentation.exception

/**
 * Exceção lançada quando os dados do bilhete são semanticamente inválidos.
 * Exemplo: stake ou odd total zerados.
 * 
 * HTTP Status: 422 Unprocessable Entity
 */
class InvalidTicketDataException(
    message: String,
    val details: Map<String, String> = emptyMap()
) : RuntimeException(message)
