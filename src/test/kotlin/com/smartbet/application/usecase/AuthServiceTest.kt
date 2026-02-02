package com.smartbet.application.usecase

import com.smartbet.application.dto.RegisterRequest
import com.smartbet.infrastructure.persistence.entity.UserEntity
import com.smartbet.infrastructure.persistence.repository.UserRepository
import com.smartbet.infrastructure.security.JwtService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.ZoneId

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val jwtService = mockk<JwtService>(relaxed = true)
    private val passwordEncoder = mockk<PasswordEncoder>(relaxed = true)

    private val authService = AuthService(userRepository, jwtService, passwordEncoder)

    @Test
    fun `register success for adult`() {
        val dob = LocalDate.now().minusYears(25).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val request = RegisterRequest(name = "Test User", email = "test@example.com", password = "password", dateOfBirth = dob)

        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns "hashed"
        every { userRepository.save(any()) } answers {
            val arg = firstArg() as UserEntity
            UserEntity(
                id = 1L,
                externalId = arg.externalId,
                email = arg.email,
                name = arg.name,
                passwordHash = arg.passwordHash,
                avatarUrl = arg.avatarUrl,
                dateOfBirth = arg.dateOfBirth,
                role = arg.role,
                isActive = arg.isActive,
                createdAt = arg.createdAt,
                updatedAt = arg.updatedAt
            )
        }
        every { jwtService.generateAccessToken(any(), any(), any()) } returns "access"
        every { jwtService.generateRefreshToken(any(), any(), any()) } returns "refresh"

        val resp = authService.register(request)

        assertEquals("access", resp.accessToken)
        assertEquals("refresh", resp.refreshToken)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `register fails when email exists`() {
        val dob = LocalDate.now().minusYears(25).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val request = RegisterRequest(name = "Test User", email = "exists@example.com", password = "password", dateOfBirth = dob)

        every { userRepository.existsByEmail(request.email) } returns true

        assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
    }

    @Test
    fun `register fails when underage`() {
        val dob = LocalDate.now().minusYears(16).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val request = RegisterRequest(name = "Young User", email = "young@example.com", password = "password", dateOfBirth = dob)

        every { userRepository.existsByEmail(request.email) } returns false

        val ex = assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        assertEquals("É necessário ter 18 anos ou mais para se cadastrar", ex.message)
    }

    @Test
    fun `register fails when dateOfBirth is null`() {
        val request = RegisterRequest(name = "Test User", email = "test@example.com", password = "password", dateOfBirth = null)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        assertEquals("Data de nascimento é obrigatória", ex.message)
    }

    @Test
    fun `register fails when dateOfBirth is in future`() {
        val dob = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val request = RegisterRequest(name = "Future User", email = "future@example.com", password = "password", dateOfBirth = dob)

        every { userRepository.existsByEmail(request.email) } returns false

        val ex = assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        assertEquals("Data de nascimento não pode ser no futuro", ex.message)
    }

    @Test
    fun `register fails when dateOfBirth is too old`() {
        val dob = LocalDate.now().minusYears(150).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val request = RegisterRequest(name = "Old User", email = "old@example.com", password = "password", dateOfBirth = dob)

        every { userRepository.existsByEmail(request.email) } returns false

        val ex = assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        assertEquals("Data de nascimento inválida", ex.message)
    }
}
