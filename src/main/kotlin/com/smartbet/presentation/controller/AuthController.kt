package com.smartbet.presentation.controller

import com.smartbet.application.dto.*
import com.smartbet.application.usecase.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticação")
class AuthController(
    private val authService: AuthService
) {
    
    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login do usuário")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token usando refresh token")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenRefreshResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/me")
    @Operation(summary = "Obter dados do usuário autenticado")
    fun getCurrentUser(@AuthenticationPrincipal userId: Long): ResponseEntity<UserResponse> {
        val response = authService.getCurrentUser(userId)
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "Alterar senha do usuário autenticado")
    fun changePassword(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Map<String, String>> {
        authService.changePassword(userId, request)
        return ResponseEntity.ok(mapOf("message" to "Senha alterada com sucesso"))
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Logout do usuário (invalida tokens no cliente)")
    fun logout(): ResponseEntity<Map<String, String>> {
        // JWT é stateless, logout é feito no cliente removendo os tokens
        return ResponseEntity.ok(mapOf("message" to "Logout realizado com sucesso"))
    }
}
