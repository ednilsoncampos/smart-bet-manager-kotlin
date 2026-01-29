package com.smartbet.common

/**
 * Valida que o ID não é null, lançando exceção com mensagem clara se for.
 * Substitui o uso de !! (force unwrap) com melhor rastreabilidade de erros.
 *
 * @param entityName Nome da entidade para a mensagem de erro
 * @return O valor não-nulo
 * @throws IllegalArgumentException se o valor for null
 */
fun <T : Any> T?.requireId(entityName: String): T {
    return requireNotNull(this) {
        "$entityName deve ter um ID válido. Entidade pode não ter sido persistida corretamente."
    }
}
