package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.entity.User
import com.smartbet.infrastructure.persistence.entity.UserEntity
import com.smartbet.infrastructure.persistence.repository.UserRepository
import com.smartbet.infrastructure.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {
    
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        // Verificar se email já existe
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email já cadastrado")
        }
        
        // Criar usuário
        val now = System.currentTimeMillis()
        val userEntity = UserEntity(
            name = request.name,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            createdAt = now,
            updatedAt = now
        )
        
        val savedUser = userRepository.save(userEntity)
        
        // Gerar tokens
        val accessToken = jwtService.generateAccessToken(savedUser.id!!, savedUser.email)
        val refreshToken = jwtService.generateRefreshToken(savedUser.id, savedUser.email)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getAccessTokenExpirationMs() / 1000,
            user = savedUser.toUserResponse()
        )
    }
    
    fun login(request: LoginRequest): AuthResponse {
        // Buscar usuário por email
        val userEntity = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Credenciais inválidas")
        
        // Verificar senha
        if (!passwordEncoder.matches(request.password, userEntity.passwordHash)) {
            throw IllegalArgumentException("Credenciais inválidas")
        }
        
        // Gerar tokens
        val accessToken = jwtService.generateAccessToken(userEntity.id!!, userEntity.email)
        val refreshToken = jwtService.generateRefreshToken(userEntity.id!!, userEntity.email)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getAccessTokenExpirationMs() / 1000,
            user = userEntity.toUserResponse()
        )
    }
    
    fun refreshToken(request: RefreshTokenRequest): TokenRefreshResponse {
        // Validar refresh token
        if (!jwtService.validateRefreshToken(request.refreshToken)) {
            throw IllegalArgumentException("Refresh token inválido ou expirado")
        }
        
        // Extrair userId do token
        val userId = jwtService.getUserIdFromToken(request.refreshToken)
            ?: throw IllegalArgumentException("Token inválido")
        
        // Buscar usuário
        val userEntity = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("Usuário não encontrado")
        }
        
        // Gerar novos tokens
        val accessToken = jwtService.generateAccessToken(userEntity.id!!, userEntity.email)
        val refreshToken = jwtService.generateRefreshToken(userEntity.id!!, userEntity.email)
        
        return TokenRefreshResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getAccessTokenExpirationMs() / 1000
        )
    }
    
    fun getCurrentUser(userId: Long): UserResponse {
        val userEntity = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("Usuário não encontrado")
        }
        return userEntity.toUserResponse()
    }
    
    @Transactional
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val userEntity = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("Usuário não encontrado")
        }
        
        // Verificar senha atual
        if (!passwordEncoder.matches(request.currentPassword, userEntity.passwordHash)) {
            throw IllegalArgumentException("Senha atual incorreta")
        }
        
        // Atualizar senha
        userEntity.passwordHash = passwordEncoder.encode(request.newPassword)
        userEntity.updatedAt = System.currentTimeMillis()
        userRepository.save(userEntity)
    }
    
    private fun UserEntity.toUserResponse() = UserResponse(
        id = this.id!!,
        name = this.name,
        email = this.email,
        createdAt = this.createdAt
    )
}
