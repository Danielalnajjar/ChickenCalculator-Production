package com.example.chickencalculator.interceptor

import com.example.chickencalculator.filter.CorrelationIdContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Request logging interceptor that logs incoming requests with correlation ID,
 * response status, and timing information while excluding sensitive data.
 */
@Component
class RequestLoggingInterceptor : HandlerInterceptor {
    
    private val logger = LoggerFactory.getLogger(RequestLoggingInterceptor::class.java)
    
    companion object {
        private const val START_TIME_ATTRIBUTE = "requestStartTime"
        private const val REQUEST_ID_ATTRIBUTE = "requestId"
        
        // Sensitive headers/parameters to exclude from logs
        private val SENSITIVE_HEADERS = setOf(
            "authorization", "x-auth-token", "cookie", "set-cookie",
            "x-forwarded-for", "x-real-ip", "x-api-key"
        )
        
        private val SENSITIVE_PARAMS = setOf(
            "password", "pwd", "token", "secret", "key", "credentials"
        )
        
        // Static resource patterns to log at DEBUG level
        private val STATIC_RESOURCE_PATTERNS = setOf(
            ".js", ".css", ".map", ".ico", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".woff", ".woff2", ".ttf"
        )
    }
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val startTime = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        
        // Store timing and ID in request attributes
        request.setAttribute(START_TIME_ATTRIBUTE, startTime)
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId)
        
        val correlationId = CorrelationIdContext.getCurrentCorrelationId() ?: "N/A"
        val uri = request.requestURI
        val method = request.method
        val userAgent = request.getHeader("User-Agent") ?: "Unknown"
        val remoteAddr = getClientIpAddress(request)
        
        // Determine log level based on resource type
        val isStaticResource = STATIC_RESOURCE_PATTERNS.any { uri.contains(it) }
        val isApiRequest = uri.startsWith("/api")
        
        if (isStaticResource) {
            logger.debug(
                "REQUEST [{}] [{}] {} {} from {} - UA: {}",
                correlationId, requestId, method, uri, remoteAddr, userAgent
            )
        } else {
            logger.info(
                "REQUEST [{}] [{}] {} {} from {} - UA: {}",
                correlationId, requestId, method, uri, remoteAddr, userAgent
            )
            
            // Log additional details for API requests
            if (isApiRequest) {
                logApiRequestDetails(request, correlationId, requestId)
            }
        }
        
        return true
    }
    
    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        // This runs after the controller but before view rendering
        // We'll do final logging in afterCompletion to capture the actual response
    }
    
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long ?: return
        val requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE) as? String ?: "unknown"
        
        val duration = System.currentTimeMillis() - startTime
        val correlationId = CorrelationIdContext.getCurrentCorrelationId() ?: "N/A"
        val uri = request.requestURI
        val method = request.method
        val status = response.status
        
        // Determine log level based on response status and resource type
        val isStaticResource = STATIC_RESOURCE_PATTERNS.any { uri.contains(it) }
        val isError = status >= 400
        val isServerError = status >= 500
        
        val logMessage = "RESPONSE [{}] [{}] {} {} -> {} in {}ms"
        val logArgs = arrayOf(correlationId, requestId, method, uri, status, duration)
        
        when {
            isServerError -> logger.error(logMessage, *logArgs)
            isError -> logger.warn(logMessage, *logArgs)
            isStaticResource -> logger.debug(logMessage, *logArgs)
            else -> logger.info(logMessage, *logArgs)
        }
        
        // Log exception details if present
        if (ex != null) {
            logger.error(
                "REQUEST [{}] [{}] {} {} failed with exception: {}",
                correlationId, requestId, method, uri, ex.message, ex
            )
        }
        
        // Log slow requests (> 5 seconds) as warnings
        if (duration > 5000 && !isStaticResource) {
            logger.warn(
                "SLOW REQUEST [{}] [{}] {} {} took {}ms (>5s)",
                correlationId, requestId, method, uri, duration
            )
        }
    }
    
    /**
     * Logs additional details for API requests while excluding sensitive information.
     */
    private fun logApiRequestDetails(
        request: HttpServletRequest,
        correlationId: String,
        requestId: String
    ) {
        // Log content type and length
        val contentType = request.contentType
        val contentLength = request.contentLengthLong
        
        if (contentType != null) {
            logger.debug(
                "REQUEST [{}] [{}] Content-Type: {}, Content-Length: {}",
                correlationId, requestId, contentType, 
                if (contentLength > 0) contentLength else "unknown"
            )
        }
        
        // Log safe headers (exclude sensitive ones)
        val safeHeaders = mutableMapOf<String, String>()
        request.headerNames?.asIterator()?.forEach { headerName ->
            if (!SENSITIVE_HEADERS.contains(headerName.lowercase())) {
                safeHeaders[headerName] = request.getHeader(headerName)
            }
        }
        
        if (safeHeaders.isNotEmpty()) {
            logger.debug(
                "REQUEST [{}] [{}] Headers: {}",
                correlationId, requestId, safeHeaders
            )
        }
        
        // Log safe parameters (exclude sensitive ones)
        val safeParams = mutableMapOf<String, Array<String>>()
        request.parameterNames?.asIterator()?.forEach { paramName ->
            if (!SENSITIVE_PARAMS.contains(paramName.lowercase())) {
                safeParams[paramName] = request.getParameterValues(paramName)
            }
        }
        
        if (safeParams.isNotEmpty()) {
            logger.debug(
                "REQUEST [{}] [{}] Parameters: {}",
                correlationId, requestId, safeParams
            )
        }
    }
    
    /**
     * Extracts the client IP address from various possible headers.
     */
    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim()
        }
        
        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp.trim()
        }
        
        return request.remoteAddr ?: "unknown"
    }
}