package com.smartbet.domain.exception

/**
 * Exception lançada quando o usuário tenta importar um bilhete que já existe.
 */
class DuplicateTicketException(
    val ticketId: String,
    message: String = "Bilhete $ticketId já foi importado anteriormente"
) : RuntimeException(message)
