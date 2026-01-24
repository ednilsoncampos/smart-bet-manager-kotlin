package com.smartbet.domain.entity

/**
 * Entidade de domínio representando um torneio/competição.
 */
data class Tournament(
    val id: Long? = null,

    /** ID do provider (casa de apostas) */
    val providerId: Long,

    /** ID do esporte */
    val sportId: Long,

    /** ID externo do torneio na API do provider */
    val externalId: Int,

    /** Nome do torneio - ex: "Brasileirão Série A", "Champions League" */
    val name: String,

    /** Nome da categoria/país - ex: "Brasil", "Inglaterra" */
    val localName: String? = null,

    /** Timestamp de criação (milissegundos desde epoch UTC) */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp de atualização (milissegundos desde epoch UTC) */
    val updatedAt: Long = System.currentTimeMillis()
)
