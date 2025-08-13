package com.example.chickencalculator.controller

import com.example.chickencalculator.config.ApiVersionConfig
import com.example.chickencalculator.dto.LocationAuthRequest
import com.example.chickencalculator.dto.LocationAuthResponse
import com.example.chickencalculator.security.buildJwtSetCookieHeader
import com.example.chickencalculator.service.LocationAuthService
import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("${ApiVersionConfig.API_VERSION}/location")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://*.railway.app"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "X-Requested-With"]
)
class LocationAuthController(
    private val locationAuthService: LocationAuthService,
    private val metricsService: MetricsService
) {
    private val logger = LoggerFactory.getLogger(LocationAuthController::class.java)
    
    companion object {
        const val COOKIE_MAX_AGE = 8 * 60 * 60 // 8 hours in seconds
    }
    
    /**
     * Login to a specific location
     */
    @PostMapping("/{slug}/auth/login")
    @Timed(value = "chicken.calculator.location.auth.login.time", description = "Time taken for location login")
    fun login(
        @PathVariable slug: String,
        @RequestBody request: LocationAuthRequest,
        response: HttpServletResponse
    ): ResponseEntity<LocationAuthResponse> {
        val startTime = System.currentTimeMillis()
        
        return try {
            // Authenticate location
            val token = locationAuthService.authenticateLocation(slug, request.password)
            
            // Create location-specific cookie with SameSite
            val setCookie = buildJwtSetCookieHeader(
                "${LocationAuthService.TOKEN_PREFIX}$slug", 
                token, 
                Duration.ofSeconds(COOKIE_MAX_AGE.toLong())
            )
            
            // Record metrics
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordLocationAccess(slug, "login")
            metricsService.recordDatabaseOperation("location_auth_login", processingTime)
            
            logger.info("Successful login for location: $slug")
            
            ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, setCookie)
                .body(LocationAuthResponse(
                success = true,
                message = "Login successful",
                slug = slug,
                expiresIn = COOKIE_MAX_AGE
            ))
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordError("location_auth_login", e.javaClass.simpleName, slug)
            metricsService.recordDatabaseOperation("location_auth_login_failed", processingTime)
            
            logger.warn("Failed login attempt for location: $slug - ${e.message}")
            throw e
        }
    }
    
    /**
     * Logout from a specific location
     */
    @PostMapping("/{slug}/auth/logout")
    @Timed(value = "chicken.calculator.location.auth.logout.time", description = "Time taken for location logout")
    fun logout(
        @PathVariable slug: String,
        response: HttpServletResponse
    ): ResponseEntity<Map<String, Any>> {
        // Clear the location-specific cookie with SameSite
        val expiredCookie = buildJwtSetCookieHeader(
            "${LocationAuthService.TOKEN_PREFIX}$slug",
            "",
            Duration.ZERO
        )
        
        metricsService.recordLocationAccess(slug, "logout")
        logger.info("Logout for location: $slug")
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredCookie)
            .body(mapOf(
            "success" to true,
            "message" to "Logged out successfully"
        ))
    }
    
    /**
     * Validate current session for a location
     */
    @GetMapping("/{slug}/auth/validate")
    @Timed(value = "chicken.calculator.location.auth.validate.time", description = "Time taken to validate location session")
    fun validateSession(
        @PathVariable slug: String,
        httpRequest: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        // Get the location-specific cookie
        val cookieName = "${LocationAuthService.TOKEN_PREFIX}$slug"
        val token = httpRequest.cookies?.find { it.name == cookieName }?.value
        
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf(
                "valid" to false,
                "message" to "No session found"
            ))
        }
        
        val claims = locationAuthService.validateLocationToken(token)
        if (claims == null || claims.subject != slug) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf(
                "valid" to false,
                "message" to "Invalid or expired session"
            ))
        }
        
        return ResponseEntity.ok(mapOf(
            "valid" to true,
            "locationId" to (claims["locationId"] ?: 0),
            "locationName" to (claims["locationName"] ?: ""),
            "expiresAt" to claims.expiration.time
        ))
    }
    
    /**
     * Check if a location requires authentication
     */
    @GetMapping("/{slug}/auth/required")
    fun checkAuthRequired(@PathVariable slug: String): ResponseEntity<Map<String, Any>> {
        val location = locationAuthService.getLocationBySlug(slug)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "error" to "Location not found"
            ))
        
        return ResponseEntity.ok(mapOf(
            "requiresAuth" to location.requiresAuth,
            "locationName" to location.name
        ))
    }
}