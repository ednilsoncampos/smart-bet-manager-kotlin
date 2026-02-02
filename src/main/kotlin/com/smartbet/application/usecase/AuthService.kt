package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.infrastructure.persistence.entity.UserEntity
import com.smartbet.infrastructure.persistence.repository.UserRepository
import com.smartbet.infrastructure.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

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
        
        // Validar maioridade
        val dateOfBirth = request.dateOfBirth
            ?: throw IllegalArgumentException("Data de nascimento é obrigatória")

        val dobInstant = Instant.ofEpochMilli(dateOfBirth)
        val dobLocalDate = dobInstant.atZone(ZoneId.systemDefault()).toLocalDate()

        // Validar data futura
        if (dobLocalDate.isAfter(LocalDate.now())) {
            throw IllegalArgumentException("Data de nascimento não pode ser no futuro")
        }

        val age = Period.between(dobLocalDate, LocalDate.now()).years

        // Validar data muito antiga
        if (age > 120) {
            throw IllegalArgumentException("Data de nascimento inválida")
        }

        if (age < 18) {
            throw IllegalArgumentException("É necessário ter 18 anos ou mais para se cadastrar")
        }

        // Criar usuário
        val now = System.currentTimeMillis()
        val userEntity = UserEntity(
            name = request.name,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            dateOfBirth = dateOfBirth,
            createdAt = now,
            updatedAt = now
        )
        
        val savedUser = userRepository.save(userEntity)
        val savedUserId = savedUser.id ?: throw IllegalStateException("Falha ao cadastrar usuário.")

        // Gerar tokens
        val accessToken = jwtService.generateAccessToken(savedUserId, savedUser.email, savedUser.role.name)
        val refreshToken = jwtService.generateRefreshToken(savedUserId, savedUser.email, savedUser.role.name)

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
        
        val userId = userEntity.id ?: throw IllegalStateException("User id is null")
        // Gerar tokens
        val accessToken = jwtService.generateAccessToken(userId, userEntity.email, userEntity.role.name)
        val refreshToken = jwtService.generateRefreshToken(userId, userEntity.email, userEntity.role.name)

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
        val userIdFromToken = jwtService.getUserIdFromToken(request.refreshToken)
            ?: throw IllegalArgumentException("Token inválido")
        
        // Buscar usuário
        val userEntity = userRepository.findById(userIdFromToken).orElseThrow {
            IllegalArgumentException("Usuário não encontrado")
        }
        
        val userId = userEntity.id ?: throw IllegalStateException("User id is null")
        // Gerar novos tokens
        val accessToken = jwtService.generateAccessToken(userId, userEntity.email, userEntity.role.name)
        val refreshToken = jwtService.generateRefreshToken(userId, userEntity.email, userEntity.role.name)

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
        createdAt = this.createdAt,
        dateOfBirth = this.dateOfBirth
    )
}
