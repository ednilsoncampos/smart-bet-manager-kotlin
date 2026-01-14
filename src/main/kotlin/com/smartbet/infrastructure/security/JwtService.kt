package com.smartbet.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret:smartbet-secret-key-must-be-at-least-256-bits-long}")
    private val secretKey: String,
    
    @Value("\${jwt.access-token-expiration:3600000}") // 1 hora
    private val accessTokenExpiration: Long,
    
    @Value("\${jwt.refresh-token-expiration:604800000}") // 7 dias
    private val refreshTokenExpiration: Long
) {
    
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray())
    }
    
    fun generateAccessToken(userId: Long, email: String): String {
        return generateToken(userId, email, accessTokenExpiration, "access")
    }
    
    fun generateRefreshToken(userId: Long, email: String): String {
        return generateToken(userId, email, refreshTokenExpiration, "refresh")
    }
    
    private fun generateToken(userId: Long, email: String, expiration: Long, type: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)
        
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }
    
    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }
    
    fun validateAccessToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            val type = claims["type"] as? String
            type == "access" && !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }
    
    fun validateRefreshToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            val type = claims["type"] as? String
            type == "refresh" && !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }
    
    fun getUserIdFromToken(token: String): Long? {
        return try {
            val claims = getClaims(token)
            claims.subject.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    fun getEmailFromToken(token: String): String? {
        return try {
            val claims = getClaims(token)
            claims["email"] as? String
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
    
    fun getAccessTokenExpirationMs(): Long = accessTokenExpiration
    fun getRefreshTokenExpirationMs(): Long = refreshTokenExpiration
}
