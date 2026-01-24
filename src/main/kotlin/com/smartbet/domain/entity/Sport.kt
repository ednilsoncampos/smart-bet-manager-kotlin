package com.smartbet.domain.entity

/**
 * Entidade de domínio representando um esporte.
 */
data class Sport(
    val id: Long? = null,

    /** ID do provider (casa de apostas) */
    val providerId: Long,

    /** ID externo do esporte na API do provider */
    val externalId: Int,

    /** Nome do esporte - ex: "Futebol", "Basquete" */
    val name: String,

    /** Timestamp de criação (milissegundos desde epoch UTC) */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp de atualização (milissegundos desde epoch UTC) */
    val updatedAt: Long = System.currentTimeMillis()
)
