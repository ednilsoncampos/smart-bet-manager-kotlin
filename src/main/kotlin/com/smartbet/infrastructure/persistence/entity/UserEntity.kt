package com.smartbet.infrastructure.persistence.entity

import com.smartbet.domain.entity.User
import com.smartbet.domain.entity.UserRole
import jakarta.persistence.*

@Entity
@Table(name = "users", schema = "core")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "external_id", unique = true)
    var externalId: String? = null,
    
    @Column(nullable = false, unique = true)
    var email: String = "",
    
    @Column(nullable = false)
    var name: String = "",
    
    @Column(name = "password_hash")
    var passwordHash: String? = null,
    
    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "date_of_birth")
    var dateOfBirth: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long = System.currentTimeMillis(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): User = User(
        id = id,
        externalId = externalId,
        email = email,
        name = name,
        dateOfBirth = dateOfBirth,
        avatarUrl = avatarUrl,
        role = role,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            externalId = user.externalId,
            email = user.email,
            name = user.name,
            avatarUrl = user.avatarUrl,
            dateOfBirth = user.dateOfBirth,
            role = user.role,
            isActive = user.isActive,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
    
    @PreUpdate
    fun preUpdate() {
        updatedAt = System.currentTimeMillis()
    }
}
