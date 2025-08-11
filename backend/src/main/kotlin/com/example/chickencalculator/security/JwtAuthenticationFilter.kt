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
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)
        
        try {
            if (jwtService.validateToken(jwt)) {
                val email = jwtService.getEmailFromToken(jwt)
                val role = jwtService.getRoleFromToken(jwt)
                
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
                }
            }
        } catch (e: Exception) {
            logger.error("JWT authentication failed", e)
        }
        
        filterChain.doFilter(request, response)
    }
}