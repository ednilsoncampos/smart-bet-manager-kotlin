package com.smartbet.infrastructure.config

import com.smartbet.infrastructure.security.JwtAuthenticationFilter
import com.smartbet.infrastructure.security.RateLimitFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: String
) {

    companion object {
        const val CORS_MAX_AGE_SECONDS = 3600L
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Liberar preflight OPTIONS para todas as rotas (CORS)
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    
                    // Endpoints de teste
                    .requestMatchers("/api/test/**").permitAll()
                    
                    // Endpoints de autenticação (públicos)
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/register").permitAll()
                    .requestMatchers("/api/auth/refresh").permitAll()
                    
                    // Endpoints públicos
                    .requestMatchers("/api/health").permitAll()
                    .requestMatchers("/api/providers").permitAll()
                    .requestMatchers("/api/providers/check-url").permitAll()
                    
                    // Swagger/OpenAPI
                    .requestMatchers("/api-docs/**").permitAll()
                    .requestMatchers("/api-docs.yaml").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/swagger-ui.html").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    
                    // Actuator (health checks, metrics)
                    .requestMatchers("/actuator/**").permitAll()

                    // Endpoints de administração (apenas ADMIN)
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // Todos os outros endpoints requerem autenticação
                    .anyRequest().authenticated()
            }
            // Adicionar rate limit antes de tudo para bloquear requisições excedentes rapidamente
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
            // Adicionar filtro JWT para autenticação
            .addFilterAfter(jwtAuthenticationFilter, RateLimitFilter::class.java)
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

        return http.build()
    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = allowedOrigins.split(",").map { it.trim() }

        val configuration = CorsConfiguration().apply {
            // Convertendo string separada por vírgula em lista
            // Ex: "http://localhost:3000,https://smartbet.api.br"
            allowedOrigins = origins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization", "Content-Type")
            allowCredentials = true
            maxAge = CORS_MAX_AGE_SECONDS
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
