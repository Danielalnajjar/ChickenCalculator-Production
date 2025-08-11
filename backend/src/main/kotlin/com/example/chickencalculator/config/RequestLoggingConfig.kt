package com.example.chickencalculator.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import java.io.File

@Configuration
class RequestLoggingConfig {
    private val logger = LoggerFactory.getLogger(RequestLoggingConfig::class.java)
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun loggingFilter(): Filter {
        return Filter { request: ServletRequest, response: ServletResponse, chain: FilterChain ->
            val httpRequest = request as HttpServletRequest
            val httpResponse = response as HttpServletResponse
            val uri = httpRequest.requestURI
            
            logger.info("🔍 REQUEST: ${httpRequest.method} $uri")
            logger.debug("   Headers: ${httpRequest.headerNames.toList().map { "$it: ${httpRequest.getHeader(it)}" }}")
            logger.debug("   From: ${httpRequest.remoteAddr}")
            
            // Special logging for static resource requests
            if (uri.contains("/static/") || uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".map")) {
                logger.info("📦 Static resource requested: $uri")
                
                // Check if the file exists at various possible locations
                val possiblePaths = listOf(
                    "/app/static/admin${uri.removePrefix("/admin")}",
                    "/app/static/admin$uri",
                    "/app/static/app$uri",
                    "/app$uri"
                )
                
                var found = false
                possiblePaths.forEach { path ->
                    val file = File(path)
                    if (file.exists() && file.isFile) {
                        logger.info("   ✅ File EXISTS at: $path (size: ${file.length()} bytes)")
                        found = true
                    }
                }
                
                if (!found) {
                    logger.warn("   ❌ File NOT FOUND in any expected location for: $uri")
                }
            }
            
            // Log if this is an API request
            if (uri.startsWith("/api")) {
                logger.debug("   ✅ This is an API request - should reach controller")
            }
            
            chain.doFilter(request, response)
            
            // Log response status for static resources
            if (uri.contains("/static/") || uri.endsWith(".js") || uri.endsWith(".css")) {
                logger.info("📤 RESPONSE for $uri: Status ${httpResponse.status}")
            }
        }
    }
}