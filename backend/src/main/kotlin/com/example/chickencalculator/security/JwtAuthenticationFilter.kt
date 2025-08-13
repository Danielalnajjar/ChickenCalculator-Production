package com.example.chickencalculator.security

import com.example.chickencalculator.service.JwtService
import com.example.chickencalculator.util.PathUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    /**
     * Prevent filter from being applied during error dispatch to avoid recursive issues
     */
    override fun shouldNotFilterErrorDispatch(): Boolean = true
    
    /**
     * Skip certain paths that don't need JWT authentication
     */
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
               path.startsWith("/api/v1/location")  // Location paths use different auth
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val requestPath = PathUtil.normalizedPath(request)
            
            // Clear any existing authentication context to start fresh
            SecurityContextHolder.clearContext()
            
            // Try to get JWT token from cookie first, then Authorization header
            val jwt = getTokenFromRequest(request)
            
            if (jwt == null) {
                logger.debug("No JWT token found for request: $requestPath")
                filterChain.doFilter(request, response)
                return
            }

            logger.debug("JWT token found for request: $requestPath")
            
            try {
                if (jwtService.validateToken(jwt)) {
                    val email = jwtService.getEmailFromToken(jwt)
                    val role = jwtService.getRoleFromToken(jwt)
                    
                    logger.debug("JWT validated successfully - Email: $email, Role: $role, Path: $requestPath")
                    
                    if (email != null) {
                        val authorities = if (role != null) {
                            listOf(SimpleGrantedAuthority("ROLE_$role"))
                        } else {
                            emptyList()
                        }
                        
                        val authToken = UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                        )
                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authToken
                        logger.debug("Authentication set in SecurityContext for: $email")
                    }
                } else {
                    logger.warn("JWT validation failed for request: $requestPath")
                    // Clear context on validation failure
                    SecurityContextHolder.clearContext()
                }
            } catch (jwtException: Exception) {
                logger.error("JWT processing failed for request: $requestPath - ${jwtException.message}")
                // Clear context on any JWT processing error
                SecurityContextHolder.clearContext()
            }
            
        } catch (filterException: Exception) {
            logger.error("Filter processing failed for request: ${PathUtil.normalizedPath(request)} - ${filterException.message}", filterException)
            // Clear context on any filter error to ensure clean state
            SecurityContextHolder.clearContext()
        } finally {
            // Always continue the filter chain, never block the request
            try {
                filterChain.doFilter(request, response)
            } catch (chainException: Exception) {
                logger.error("Filter chain execution failed: ${chainException.message}", chainException)
                // Don't re-throw - let the request continue to avoid 500 errors
            }
        }
    }
    
    /**
     * Extract JWT token from cookie or Authorization header
     */
    private fun getTokenFromRequest(request: HttpServletRequest): String? {
        // First, try to get token from cookie
        request.cookies?.let { cookies ->
            for (cookie in cookies) {
                if (cookie.name == "jwt_token") {
                    return cookie.value
                }
            }
        }
        
        // Fallback to Authorization header for backward compatibility
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }
        
        return null
    }
}