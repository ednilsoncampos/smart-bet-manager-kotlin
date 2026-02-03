package com.smartbet.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.NotNull

// === Request DTOs ===

data class LoginRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,
    
    @field:NotBlank(message = "Senha é obrigatória")
    val password: String
)

data class RegisterRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    val name: String,
    
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,
    
    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
    val password: String,

    @field:NotNull(message = "Data de nascimento é obrigatória")
    val dateOfBirth: Long?
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token é obrigatório")
    val refreshToken: String
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Senha atual é obrigatória")
    val currentPassword: String,
    
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 6, max = 100, message = "Nova senha deve ter entre 6 e 100 caracteres")
    val newPassword: String
)

// === Response DTOs ===

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: Long,
    val dateOfBirth: Long? = null
)

data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)
