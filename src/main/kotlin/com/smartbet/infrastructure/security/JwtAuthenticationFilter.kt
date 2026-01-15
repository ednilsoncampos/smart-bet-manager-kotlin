package com.smartbet.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    
    companion object {
        private val PUBLIC_PATHS = listOf(
            "/api/health",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/providers",
            "/api/providers/check-url",
            "/api/test",
            "/api-docs",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
        )
    }
    
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        val method = request.method
        
        // Skip filter for OPTIONS requests (CORS preflight)
        if (method == "OPTIONS") {
            return true
        }
        
        // Skip filter for public paths
        return PUBLIC_PATHS.any { path.startsWith(it) }
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)
            
            if (token != null && jwtService.validateAccessToken(token)) {
                val userId = jwtService.getUserIdFromToken(token)
                
                if (userId != null) {
                    val authentication = UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        emptyList()
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (e: Exception) {
            logger.error("Erro ao processar token JWT: ${e.message}")
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
