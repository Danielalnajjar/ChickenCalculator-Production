package com.example.chickencalculator.filter

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.*

/**
 * Servlet filter that adds a correlation ID to every incoming request.
 * The correlation ID is stored in MDC (Mapped Diagnostic Context) for thread-local access
 * and added to response headers for client-side tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : Filter {
    
    private val logger = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
    
    companion object {
        const val CORRELATION_ID_HEADER_NAME = "X-Correlation-ID"
        const val CORRELATION_ID_MDC_KEY = "correlationId"
        private const val CORRELATION_ID_REQUEST_ATTRIBUTE = "correlationId"
    }
    
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        
        val correlationId = generateOrExtractCorrelationId(httpRequest)
        
        try {
            // Store correlation ID in MDC for thread-local access
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
            
            // Store in request attributes for controller access
            httpRequest.setAttribute(CORRELATION_ID_REQUEST_ATTRIBUTE, correlationId)
            
            // Add to response headers for client-side tracing
            httpResponse.setHeader(CORRELATION_ID_HEADER_NAME, correlationId)
            
            logger.debug("Correlation ID {} assigned to request {} {}", 
                correlationId, httpRequest.method, httpRequest.requestURI)
            
            // Continue with the filter chain
            chain.doFilter(request, response)
            
        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }
    
    /**
     * Generates a new correlation ID or extracts one from the request headers.
     * If the client provides a correlation ID in the request header, it will be used.
     * Otherwise, a new UUID will be generated.
     */
    private fun generateOrExtractCorrelationId(request: HttpServletRequest): String {
        // First, check if the client provided a correlation ID
        val existingCorrelationId = request.getHeader(CORRELATION_ID_HEADER_NAME)
        
        return if (!existingCorrelationId.isNullOrBlank() && isValidCorrelationId(existingCorrelationId)) {
            logger.debug("Using existing correlation ID from request header: {}", existingCorrelationId)
            existingCorrelationId.trim()
        } else {
            // Generate a new correlation ID
            val newCorrelationId = UUID.randomUUID().toString()
            logger.debug("Generated new correlation ID: {}", newCorrelationId)
            newCorrelationId
        }
    }
    
    /**
     * Validates that the correlation ID is in a proper format.
     * Accepts UUIDs and alphanumeric strings between 8-64 characters.
     */
    private fun isValidCorrelationId(correlationId: String): Boolean {
        return correlationId.length in 8..64 && 
               correlationId.matches(Regex("^[a-zA-Z0-9-]+$"))
    }
    
    override fun init(filterConfig: FilterConfig?) {
        logger.info("CorrelationIdFilter initialized - All requests will receive correlation IDs")
    }
    
    override fun destroy() {
        logger.info("CorrelationIdFilter destroyed")
    }
}

/**
 * Utility object for accessing correlation ID from anywhere in the application.
 */
object CorrelationIdContext {
    
    /**
     * Gets the current correlation ID from MDC.
     * Returns null if no correlation ID is set.
     */
    fun getCurrentCorrelationId(): String? {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
    }
    
    /**
     * Gets the current correlation ID from MDC or generates a new one if not present.
     * This is useful for operations that might run outside of the request context.
     */
    fun getOrGenerateCorrelationId(): String {
        return getCurrentCorrelationId() ?: UUID.randomUUID().toString().also { newId ->
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, newId)
        }
    }
    
    /**
     * Sets a correlation ID in the MDC.
     * Use with caution - typically the filter handles this automatically.
     */
    fun setCorrelationId(correlationId: String) {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId)
    }
    
    /**
     * Removes the correlation ID from MDC.
     * Use with caution - typically the filter handles this automatically.
     */
    fun clearCorrelationId() {
        MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
    }
}