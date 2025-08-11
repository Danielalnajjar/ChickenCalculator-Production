package com.example.chickencalculator.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*

@Service
class JwtService {
    
    // Generate a secure key for HS256
    private val key: Key = Keys.secretKeyFor(SignatureAlgorithm.HS256)
    
    @Value("\${jwt.expiration:86400000}") // Default 24 hours
    private val jwtExpiration: Long = 86400000
    
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