package com.example.chickencalculator.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@Configuration
class RequestLoggingConfig {
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun loggingFilter(): Filter {
        return Filter { request: ServletRequest, response: ServletResponse, chain: FilterChain ->
            val httpRequest = request as HttpServletRequest
            println("🔍 REQUEST: ${httpRequest.method} ${httpRequest.requestURI}")
            println("   Headers: ${httpRequest.headerNames.toList().map { "$it: ${httpRequest.getHeader(it)}" }}")
            println("   From: ${httpRequest.remoteAddr}")
            
            // Log if this is an API request
            if (httpRequest.requestURI.startsWith("/api")) {
                println("   ✅ This is an API request - should reach controller")
            }
            
            chain.doFilter(request, response)
        }
    }
}