package com.example.chickencalculator.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory

@Service
class JwtService {
    
    private val logger = LoggerFactory.getLogger(JwtService::class.java)
    
    // Use persistent key from environment or generate a secure default for development
    private val key: Key = initializeKey()
    
    @Value("\${jwt.expiration:86400000}") // Default 24 hours
    private val jwtExpiration: Long = 86400000
    
    private fun initializeKey(): Key {
        // Get JWT secret from environment variable (primary method)
        val envSecret = System.getenv("JWT_SECRET")
        
        if (envSecret.isNullOrBlank()) {
            logger.error("❌ CRITICAL SECURITY ERROR: JWT_SECRET environment variable is not set!")
            throw IllegalStateException("JWT_SECRET environment variable must be configured for security")
        }
        
        if (envSecret.length < 32) {
            logger.error("❌ CRITICAL SECURITY ERROR: JWT_SECRET must be at least 32 characters long!")
            throw IllegalStateException("JWT_SECRET must be at least 32 characters for security")
        }
        
        logger.info("✅ JWT_SECRET loaded successfully (${envSecret.length} characters)")
        return SecretKeySpec(envSecret.toByteArray(), SignatureAlgorithm.HS256.jcaName)
    }
    
    fun generateToken(email: String, userId: Long, role: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)
        
        return Jwts.builder()
            .setSubject(email)
            .claim("userId", userId)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key)
            .compact()
    }
    
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getEmailFromToken(token: String): String? {
        return try {
            val claims = getClaimsFromToken(token)
            claims?.subject
        } catch (e: Exception) {
            null
        }
    }
    
    fun getUserIdFromToken(token: String): Long? {
        return try {
            val claims = getClaimsFromToken(token)
            claims?.get("userId", Long::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getRoleFromToken(token: String): String? {
        return try {
            val claims = getClaimsFromToken(token)
            claims?.get("role", String::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getClaimsFromToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            null
        }
    }
}