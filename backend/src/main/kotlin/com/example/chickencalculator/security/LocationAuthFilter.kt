package com.example.chickencalculator.security

import com.example.chickencalculator.service.LocationAuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter to validate location-based authentication for protected routes
 */
@Component
class LocationAuthFilter(
    private val locationAuthService: LocationAuthService
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
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        
        // Skip if path is excluded
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response)
            return
        }
        
        // Check if path requires location authentication
        val locationSlug = extractLocationSlug(path)
        if (locationSlug != null && requiresLocationAuth(path)) {
            // Get the location-specific cookie
            val cookieName = "${LocationAuthService.TOKEN_PREFIX}$locationSlug"
            val token = request.cookies?.find { it.name == cookieName }?.value
            
            if (token == null) {
                logger.debug("No authentication token found for location: $locationSlug")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")
                return
            }
            
            // Validate the token
            val claims = locationAuthService.validateLocationToken(token)
            if (claims == null || claims.subject != locationSlug) {
                logger.warn("Invalid token for location: $locationSlug")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired session")
                return
            }
            
            // Add location context to request attributes
            request.setAttribute("locationSlug", locationSlug)
            request.setAttribute("locationId", claims["locationId"])
            request.setAttribute("locationName", claims["locationName"])
            
            // Add headers for downstream services
            val wrapper = HeaderModifiableHttpServletRequest(request)
            wrapper.addHeader("X-Location-Id", claims["locationId"].toString())
            wrapper.addHeader("X-Location-Slug", locationSlug)
            wrapper.addHeader("X-Location-Name", claims["locationName"].toString())
            
            logger.debug("Authenticated request for location: $locationSlug")
            filterChain.doFilter(wrapper, response)
        } else {
            // No location authentication required
            filterChain.doFilter(request, response)
        }
    }
    
    /**
     * Check if path is excluded from location authentication
     */
    private fun isExcludedPath(path: String): Boolean {
        return EXCLUDED_PATTERNS.any { it.matches(path) }
    }
    
    /**
     * Check if path requires location authentication
     */
    private fun requiresLocationAuth(path: String): Boolean {
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

/**
 * Wrapper to allow adding headers to the request
 */
class HeaderModifiableHttpServletRequest(request: HttpServletRequest) : 
    jakarta.servlet.http.HttpServletRequestWrapper(request) {
    
    private val customHeaders = mutableMapOf<String, String>()
    
    fun addHeader(name: String, value: String) {
        customHeaders[name] = value
    }
    
    override fun getHeader(name: String): String? {
        return customHeaders[name] ?: super.getHeader(name)
    }
    
    override fun getHeaderNames(): java.util.Enumeration<String> {
        val headerNames = mutableListOf<String>()
        super.getHeaderNames().asIterator().forEach { headerNames.add(it) }
        headerNames.addAll(customHeaders.keys)
        return java.util.Collections.enumeration(headerNames)
    }
}