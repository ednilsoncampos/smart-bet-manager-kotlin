package com.smartbet.domain.entity

/**
 * Entidade de domínio representando um usuário do sistema.
 */
data class User(
    val id: Long? = null,
    
    /** ID externo (OAuth) */
    val externalId: String? = null,
    
    /** Email do usuário */
    val email: String,
    
    /** Nome do usuário */
    val name: String,

    /** Timestamp de data de nascimento (milissegundos desde epoch UTC) */
    val dateOfBirth: Long? = null,

    /** URL do avatar */
    val avatarUrl: String? = null,
    
    /** Role do usuário */
    val role: UserRole = UserRole.USER,
    
    /** Se o usuário está ativo */
    val isActive: Boolean = true,
    
    /** Timestamp de criação (milissegundos desde epoch UTC) */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Timestamp de atualização (milissegundos desde epoch UTC) */
    val updatedAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    USER,
    ADMIN
}
