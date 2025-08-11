package com.example.chickencalculator.security

import com.example.chickencalculator.service.JwtService
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

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        val requestPath = request.requestURI
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No JWT token found for request: $requestPath")
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)
        logger.debug("JWT token found for request: $requestPath")
        
        try {
            if (jwtService.validateToken(jwt)) {
                val email = jwtService.getEmailFromToken(jwt)
                val role = jwtService.getRoleFromToken(jwt)
                
                logger.debug("JWT validated successfully - Email: $email, Role: $role, Path: $requestPath")
                
                if (email != null && SecurityContextHolder.getContext().authentication == null) {
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
            }
        } catch (e: Exception) {
            logger.error("JWT authentication failed for request: $requestPath - ${e.message}", e)
        }
        
        filterChain.doFilter(request, response)
    }
}