package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.repository.LocationRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey

@Service
class LocationAuthService(
    private val locationRepository: LocationRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${JWT_SECRET:default-secret-key-for-development-only-change-in-production}")
    private val jwtSecret: String
) {
    private val logger = LoggerFactory.getLogger(LocationAuthService::class.java)
    
    companion object {
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 15
        const val TOKEN_PREFIX = "location_token_"
    }
    
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }
    
    /**
     * Authenticate a location with password
     */
    @Transactional
    fun authenticateLocation(slug: String, password: String): String {
        val location = locationRepository.findBySlug(slug)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        
        // Check if location is active
        if (location.status != LocationStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Location is not active")
        }
        
        // Check if authentication is required
        if (!location.requiresAuth) {
            logger.info("Location $slug does not require authentication")
            return generateLocationToken(location)
        }
        
        // Check for account lockout
        if (isLockedOut(location)) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS, 
                "Too many failed attempts. Please try again later."
            )
        }
        
        // Validate password
        if (location.passwordHash == null) {
            logger.error("Location $slug has authentication enabled but no password set")
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Location authentication not properly configured"
            )
        }
        
        if (!passwordEncoder.matches(password, location.passwordHash)) {
            // Record failed attempt
            recordFailedLogin(location)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password")
        }
        
        // Reset failed attempts on successful login
        if (location.failedLoginAttempts > 0) {
            resetFailedAttempts(location)
        }
        
        logger.info("Location $slug authenticated successfully")
        return generateLocationToken(location)
    }
    
    /**
     * Generate JWT token for location
     */
    private fun generateLocationToken(location: Location): String {
        val now = Date()
        val expiryDate = Date.from(
            LocalDateTime.now()
                .plusHours(location.sessionTimeoutHours.toLong())
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
        
        return Jwts.builder()
            .setSubject(location.slug)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("locationId", location.id)
            .claim("locationName", location.name)
            .claim("type", "location")
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact()
    }
    
    /**
     * Validate location JWT token
     */
    fun validateLocationToken(token: String): Claims? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
            
            // Verify this is a location token
            if (claims["type"] != "location") {
                logger.warn("Token is not a location token")
                return null
            }
            
            claims
        } catch (e: Exception) {
            logger.debug("Token validation failed: ${e.message}")
            null
        }
    }
    
    /**
     * Extract location ID from token
     */
    fun getLocationIdFromToken(token: String): Long? {
        val claims = validateLocationToken(token) ?: return null
        return claims["locationId"]?.toString()?.toLongOrNull()
    }
    
    /**
     * Extract location slug from token
     */
    fun getLocationSlugFromToken(token: String): String? {
        val claims = validateLocationToken(token) ?: return null
        return claims.subject
    }
    
    /**
     * Change location password (admin only)
     */
    @Transactional
    fun changeLocationPassword(locationId: Long, newPassword: String): Location {
        val location = locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        }
        
        val hashedPassword = passwordEncoder.encode(newPassword)
        val updatedLocation = location.copy(
            passwordHash = hashedPassword,
            lastPasswordChange = LocalDateTime.now(),
            failedLoginAttempts = 0,
            lastFailedLogin = null
        )
        
        logger.info("Password changed for location: ${location.slug}")
        return locationRepository.save(updatedLocation)
    }
    
    /**
     * Set initial password for location (used during setup)
     */
    @Transactional
    fun setInitialPassword(locationId: Long, password: String): Location {
        val location = locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        }
        
        // Only allow setting password if it's not already set
        if (location.passwordHash != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT, 
                "Password already set for this location"
            )
        }
        
        val hashedPassword = passwordEncoder.encode(password)
        val updatedLocation = location.copy(
            passwordHash = hashedPassword,
            lastPasswordChange = LocalDateTime.now()
        )
        
        logger.info("Initial password set for location: ${location.slug}")
        return locationRepository.save(updatedLocation)
    }
    
    /**
     * Check if location is locked out due to too many failed attempts
     */
    private fun isLockedOut(location: Location): Boolean {
        if (location.failedLoginAttempts < MAX_LOGIN_ATTEMPTS) {
            return false
        }
        
        val lastFailedLogin = location.lastFailedLogin ?: return false
        val lockoutExpiry = lastFailedLogin.plusMinutes(LOCKOUT_DURATION_MINUTES.toLong())
        
        return LocalDateTime.now().isBefore(lockoutExpiry)
    }
    
    /**
     * Record a failed login attempt
     */
    @Transactional
    private fun recordFailedLogin(location: Location) {
        val updatedLocation = location.copy(
            failedLoginAttempts = location.failedLoginAttempts + 1,
            lastFailedLogin = LocalDateTime.now()
        )
        locationRepository.save(updatedLocation)
        
        logger.warn("Failed login attempt ${updatedLocation.failedLoginAttempts} for location: ${location.slug}")
    }
    
    /**
     * Reset failed login attempts after successful login
     */
    @Transactional
    private fun resetFailedAttempts(location: Location) {
        val updatedLocation = location.copy(
            failedLoginAttempts = 0,
            lastFailedLogin = null
        )
        locationRepository.save(updatedLocation)
    }
    
    /**
     * Verify location has authentication properly configured
     */
    fun isLocationAuthConfigured(locationId: Long): Boolean {
        val location = locationRepository.findById(locationId).orElse(null) ?: return false
        return !location.requiresAuth || location.passwordHash != null
    }
    
    /**
     * Generate a secure random password for initial setup
     */
    fun generateSecurePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
        val random = Random()
        return (1..12)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * Get location by slug
     */
    fun getLocationBySlug(slug: String): Location? {
        return locationRepository.findBySlug(slug)
    }
}