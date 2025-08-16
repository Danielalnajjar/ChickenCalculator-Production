package com.example.chickencalculator.security

import com.example.chickencalculator.service.LocationAuthService
import com.example.chickencalculator.service.LocationManagementService
import com.example.chickencalculator.security.LocationAuthenticationToken
import com.example.chickencalculator.security.LocationPrincipal
import com.example.chickencalculator.util.PathUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter to validate location-based authentication for protected routes
 */
@Component
class LocationAuthFilter(
    private val locationAuthService: LocationAuthService,
    private val locationManagementService: LocationManagementService
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(LocationAuthFilter::class.java)
    
    companion object {
        // Paths that require location authentication
        val PROTECTED_PATTERNS = listOf(
            Regex("^/([^/]+)/calculator$"),
            Regex("^/([^/]+)/sales-data"),
            Regex("^/([^/]+)/history"),
            Regex("^/api/v1/location/([^/]+)/protected")
        )
        
        // Paths that should be excluded from location auth
        val EXCLUDED_PATTERNS = listOf(
            Regex("^/admin"),
            Regex("^/api/v1/admin"),
            Regex("^/api/health"),
            Regex("^/actuator"),
            Regex("^/static"),
            Regex("^/api/v1/location/([^/]+)/auth"),
            Regex("^/$")
        )
    }
    
    override fun shouldNotFilterErrorDispatch(): Boolean = true
    
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = PathUtil.normalizedPath(request)
        return path.startsWith("/actuator") ||
               path.startsWith("/debug") ||
               path == "/minimal" ||
               path == "/test" ||
               path == "/" ||
               path.startsWith("/static") ||
               path.startsWith("/assets") ||
               path.startsWith("/favicon") ||
               path.startsWith("/api/health") ||
               path.startsWith("/api/v1/admin")  // Admin paths use different auth
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val path = PathUtil.normalizedPath(request)
            
            // Skip if excluded from location auth
            if (!isExcludedPath(path)) {
                val locationSlug = extractLocationSlug(path)
                if (locationSlug != null && requiresLocationAuth(path)) {
                    
                    // Check if location requires authentication
                    val location = locationManagementService.getLocationBySlug(locationSlug)
                    if (location != null && location.requiresAuth) {
                        val cookieName = "${LocationAuthService.TOKEN_PREFIX}$locationSlug"
                        val token = request.cookies?.find { it.name == cookieName }?.value
                        
                        if (token != null) {
                            try {
                                val claims = locationAuthService.validateLocationToken(token)
                                if (claims != null && claims.subject == locationSlug) {
                                    // Create location authentication and set in SecurityContext
                                    val principal = LocationPrincipal.fromClaims(
                                        locationId = claims["locationId"],
                                        locationSlug = locationSlug,
                                        locationName = claims["locationName"]
                                    )
                                    val authentication = LocationAuthenticationToken(principal)
                                    SecurityContextHolder.getContext().authentication = authentication
                                    
                                    logger.debug("Location authentication successful for: $locationSlug")
                                } else {
                                    logger.warn("Invalid token for location: $locationSlug")
                                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid location token")
                                    return
                                }
                            } catch (e: Exception) {
                                logger.warn("Token validation failed for location: $locationSlug", e)
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token validation failed")
                                return
                            }
                        } else {
                            logger.debug("No authentication token found for location requiring auth: $locationSlug")
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Location authentication required")
                            return
                        }
                    } else if (location != null) {
                        // Location exists but doesn't require auth - set minimal context if token present
                        val cookieName = "${LocationAuthService.TOKEN_PREFIX}$locationSlug"
                        val token = request.cookies?.find { it.name == cookieName }?.value
                        if (token != null) {
                            try {
                                val claims = locationAuthService.validateLocationToken(token)
                                if (claims != null && claims.subject == locationSlug) {
                                    val principal = LocationPrincipal.fromClaims(
                                        locationId = claims["locationId"],
                                        locationSlug = locationSlug,
                                        locationName = claims["locationName"]
                                    )
                                    val authentication = LocationAuthenticationToken(principal)
                                    SecurityContextHolder.getContext().authentication = authentication
                                }
                            } catch (e: Exception) {
                                logger.debug("Optional token validation failed for $locationSlug: ${e.message}")
                            }
                        }
                    } else {
                        logger.warn("Location not found: $locationSlug")
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Location not found")
                        return
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error in LocationAuthFilter", e)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error")
            return
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response)
    }
    
    /**
     * Check if path is excluded from location authentication
     * Using simple path checks with startsWith() for prefixes and == for exact matches
     */
    private fun isExcludedPath(path: String): Boolean {
        // Path is already normalized when passed in
        
        // Check exact matches first
        if (path == "/") return true
        
        // Check prefix matches
        return path.startsWith("/admin") ||
               path.startsWith("/api/v1/admin") ||
               path.startsWith("/api/health") ||
               path.startsWith("/actuator") ||
               path.startsWith("/static") ||
               path.startsWith("/api/v1/location/") && path.contains("/auth")
    }
    
    /**
     * Check if path requires location authentication
     * Keep existing Regex patterns as they are (they're not Ant patterns)
     */
    private fun requiresLocationAuth(path: String): Boolean {
        // Path is already normalized when passed in
        return PROTECTED_PATTERNS.any { it.matches(path) }
    }
    
    /**
     * Extract location slug from path
     */
    private fun extractLocationSlug(path: String): String? {
        // Try to match location patterns
        for (pattern in PROTECTED_PATTERNS) {
            val match = pattern.find(path)
            if (match != null && match.groups.size > 1) {
                return match.groups[1]?.value
            }
        }
        
        // Check for simple /{slug} pattern
        val parts = path.trim('/').split('/')
        if (parts.isNotEmpty() && parts[0].isNotEmpty() && !isSystemPath(parts[0])) {
            return parts[0]
        }
        
        return null
    }
    
    /**
     * Check if a path segment is a system path (not a location slug)
     */
    private fun isSystemPath(segment: String): Boolean {
        val systemPaths = setOf(
            "api", "admin", "static", "actuator", "health",
            "favicon.ico", "manifest.json", "robots.txt"
        )
        return systemPaths.contains(segment.lowercase())
    }
}