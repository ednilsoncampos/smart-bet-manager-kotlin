package com.smartbet.infrastructure.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Filtro de rate limiting para endpoints públicos.
 *
 * Limita requisições por IP para prevenir:
 * - Ataques de força bruta em login
 * - Spam de registros
 * - DoS (Denial of Service)
 *
 * NOTA: Desabilitado em ambiente DEV para facilitar testes.
 */
@Component
class RateLimitFilter(
    private val environment: Environment
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)

    // Cache de buckets por IP
    private val buckets = ConcurrentHashMap<String, Bucket>()

    companion object {
        // Endpoints que devem ter rate limiting
        private val RATE_LIMITED_ENDPOINTS = listOf(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
        )

        // Configurações de rate limit - PRODUÇÃO
        private const val LOGIN_CAPACITY = 5L
        private val LOGIN_REFILL_DURATION = Duration.ofMinutes(1)

        private const val REGISTER_CAPACITY = 3L
        private val REGISTER_REFILL_DURATION = Duration.ofHours(1)

        private const val REFRESH_CAPACITY = 10L
        private val REFRESH_REFILL_DURATION = Duration.ofMinutes(1)

        // Configurações de rate limit - DESENVOLVIMENTO
        private const val DEV_CAPACITY = 1000L
        private val DEV_REFILL_DURATION = Duration.ofMinutes(1)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI

        // Aplica rate limit apenas nos endpoints configurados
        if (!shouldApplyRateLimit(requestPath)) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = getClientIp(request)
        val bucket = resolveBucket(clientIp, requestPath)

        if (bucket.tryConsume(1)) {
            // Requisição permitida
            filterChain.doFilter(request, response)
        } else {
            // Rate limit excedido
            logger.warn("Rate limit exceeded for IP: $clientIp on endpoint: $requestPath")
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.writer.write(
                """{"error":"TOO_MANY_REQUESTS","message":"Muitas requisições. Tente novamente mais tarde."}"""
            )
        }
    }

    /**
     * Verifica se o endpoint deve ter rate limiting aplicado
     */
    private fun shouldApplyRateLimit(path: String): Boolean {
        return RATE_LIMITED_ENDPOINTS.any { path.startsWith(it) }
    }

    /**
     * Resolve ou cria um bucket para o IP e endpoint
     */
    private fun resolveBucket(ip: String, path: String): Bucket {
        val key = "$ip:$path"
        return buckets.computeIfAbsent(key) { createBucket(path) }
    }

    /**
     * Cria um novo bucket com configurações específicas por endpoint
     */
    private fun createBucket(path: String): Bucket {
        val isDev = environment.activeProfiles.contains("dev")

        val bandwidth = if (isDev) {
            // Em DEV: limites muito altos (1000 req/min) para facilitar testes
            Bandwidth.builder()
                .capacity(DEV_CAPACITY)
                .refillIntervally(DEV_CAPACITY, DEV_REFILL_DURATION)
                .build()
        } else {
            // Em PROD/STAGING: limites restritivos para segurança
            when {
                path.contains("/login") -> Bandwidth.builder()
                    .capacity(LOGIN_CAPACITY)
                    .refillIntervally(LOGIN_CAPACITY, LOGIN_REFILL_DURATION)
                    .build()
                path.contains("/register") -> Bandwidth.builder()
                    .capacity(REGISTER_CAPACITY)
                    .refillIntervally(REGISTER_CAPACITY, REGISTER_REFILL_DURATION)
                    .build()
                path.contains("/refresh") -> Bandwidth.builder()
                    .capacity(REFRESH_CAPACITY)
                    .refillIntervally(REFRESH_CAPACITY, REFRESH_REFILL_DURATION)
                    .build()
                else -> Bandwidth.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofMinutes(1))
                    .build()
            }
        }

        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }

    /**
     * Extrai o IP real do cliente, considerando proxies
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}
