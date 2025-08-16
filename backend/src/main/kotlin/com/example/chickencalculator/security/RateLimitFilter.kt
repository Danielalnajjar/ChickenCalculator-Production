package com.example.chickencalculator.security

import io.github.bucket4j.Bucket
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting filter using Bucket4j.
 * Implements 100 requests per minute per IP address for login endpoints.
 */
@Component
@Order(10) // Apply early in filter chain
class RateLimitFilter(
    @Autowired private val meterRegistry: MeterRegistry
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)
    
    // In-memory cache of buckets per IP address
    private val buckets = ConcurrentHashMap<String, Bucket>()
    
    // Rate limit counter
    private val rateLimitCounter: Counter by lazy {
        Counter.builder("rate_limit_exceeded")
            .description("Number of requests that exceeded rate limit")
            .tag("application", "chicken-calculator")
            .register(meterRegistry)
    }

    companion object {
        // Paths that are rate limited
        private val RATE_LIMITED_PATTERNS = listOf(
            Regex("^/api/v1/admin/auth/login$"),
            Regex("^/api/v1/location/[^/]+/auth/login$")
        )
        
        // Rate limit: 100 requests per minute
        private const val REQUESTS_PER_MINUTE = 100L
        private val RATE_LIMIT_DURATION = Duration.ofMinutes(1)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !RATE_LIMITED_PATTERNS.any { pattern -> pattern.matches(path) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val clientIp = getClientIpAddress(request)
        val path = request.requestURI
        
        try {
            val bucket = getBucketForIp(clientIp)
            
            if (bucket.tryConsume(1)) {
                // Request allowed
                logger.debug("Rate limit OK for IP: $clientIp, path: $path")
                filterChain.doFilter(request, response)
            } else {
                // Rate limit exceeded
                logger.warn("Rate limit exceeded for IP: $clientIp, path: $path")
                
                // Increment metrics counter
                rateLimitCounter.increment()
                
                // Calculate retry-after time (seconds until bucket refills)
                val retryAfterSeconds = calculateRetryAfter(bucket)
                
                response.status = 429 // Too Many Requests
                response.setHeader("Retry-After", retryAfterSeconds.toString())
                response.setHeader("X-RateLimit-Limit", REQUESTS_PER_MINUTE.toString())
                response.setHeader("X-RateLimit-Remaining", "0")
                response.setHeader("X-RateLimit-Reset", (System.currentTimeMillis() / 1000 + retryAfterSeconds).toString())
                response.contentType = "application/json"
                response.writer.write("""{"error":"Rate limit exceeded","retryAfter":$retryAfterSeconds}""")
                response.writer.flush()
            }
        } catch (e: Exception) {
            logger.error("Error in rate limit filter for IP: $clientIp", e)
            // Continue with request on error to avoid blocking legitimate traffic
            filterChain.doFilter(request, response)
        }
    }

    /**
     * Get or create a bucket for the given IP address
     */
    private fun getBucketForIp(ip: String): Bucket {
        return buckets.computeIfAbsent(ip) { createNewBucket() }
    }

    /**
     * Create a new rate limiting bucket with 100 requests per minute
     */
    private fun createNewBucket(): Bucket {
        val bandwidth = Bandwidth.classic(
            REQUESTS_PER_MINUTE,
            Refill.intervally(REQUESTS_PER_MINUTE, RATE_LIMIT_DURATION)
        )
        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }

    /**
     * Extract client IP address from request, considering proxies
     */
    private fun getClientIpAddress(request: HttpServletRequest): String {
        // Check various headers for real IP (useful behind proxies)
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp.trim()
        }

        val xOriginalForwardedFor = request.getHeader("X-Original-Forwarded-For")
        if (!xOriginalForwardedFor.isNullOrBlank()) {
            return xOriginalForwardedFor.split(",")[0].trim()
        }

        // Fallback to remote address
        return request.remoteAddr ?: "unknown"
    }

    /**
     * Calculate retry-after time in seconds
     */
    private fun calculateRetryAfter(bucket: Bucket): Long {
        // Simple approach: suggest waiting 60 seconds (1 minute for refill)
        return 60L
    }
}