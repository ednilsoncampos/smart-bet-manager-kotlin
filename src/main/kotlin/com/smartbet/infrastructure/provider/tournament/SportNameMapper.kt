package com.smartbet.infrastructure.provider.tournament

/**
 * Mapeamento de IDs externos de esportes para seus nomes.
 *
 * Estrutura preparada para futura integração com API de esportes.
 * Por enquanto, usa mapeamento estático.
 */
object SportNameMapper {

    private val sportNames = mapOf(
        5 to "Futebol",
        4 to "Basquete"
    )

    /**
     * Retorna o nome do esporte com base no ID externo.
     *
     * @param externalId ID externo do esporte na API do provider
     * @return Nome do esporte ou "Esporte {id}" se não encontrado
     */
    fun getName(externalId: Int): String =
        sportNames[externalId] ?: "Esporte $externalId"

    /**
     * Verifica se o esporte é suportado.
     */
    fun isSupported(externalId: Int): Boolean =
        sportNames.containsKey(externalId)

    /**
     * Retorna todos os IDs de esportes suportados.
     */
    fun getSupportedSportIds(): Set<Int> = sportNames.keys
}
