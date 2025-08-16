package com.example.chickencalculator.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.SecureRandom
import java.util.Base64

/**
 * Filter that generates per-request nonces for Content Security Policy.
 * Applies secure CSP headers without unsafe-eval for enhanced security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50) // Apply early, after other security filters
class CspNonceFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(CspNonceFilter::class.java)
    private val secureRandom = SecureRandom()

    companion object {
        const val CSP_NONCE_ATTRIBUTE = "csp-nonce"
        private const val NONCE_LENGTH = 16 // 128-bit nonce
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        // Skip CSP for non-browser requests (API endpoints)
        return path.startsWith("/api/") || 
               path.startsWith("/actuator/") ||
               path.endsWith(".js") ||
               path.endsWith(".css") ||
               path.endsWith(".json") ||
               path.endsWith(".xml")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Generate a cryptographically secure nonce for this request
            val nonce = generateNonce()
            
            // Store nonce in request attribute for use by templates
            request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce)
            
            // Build and set CSP header with the nonce
            val cspPolicy = buildCspPolicy(nonce)
            response.setHeader("Content-Security-Policy", cspPolicy)
            
            logger.debug("Applied CSP with nonce: ${nonce.take(8)}...")
            
        } catch (e: Exception) {
            logger.error("Error generating CSP nonce", e)
            // Continue without CSP rather than blocking the request
        }
        
        filterChain.doFilter(request, response)
    }

    /**
     * Generate a cryptographically secure base64-encoded nonce
     */
    private fun generateNonce(): String {
        val nonceBytes = ByteArray(NONCE_LENGTH)
        secureRandom.nextBytes(nonceBytes)
        return Base64.getEncoder().encodeToString(nonceBytes)
    }

    /**
     * Build Content Security Policy with nonce and no unsafe-eval
     */
    private fun buildCspPolicy(nonce: String): String {
        return buildString {
            append("default-src 'self'; ")
            append("script-src 'self' 'nonce-$nonce'; ")
            append("style-src 'self' 'nonce-$nonce' 'unsafe-inline'; ") // Allow inline styles for React/CSS-in-JS
            append("img-src 'self' data: https:; ")
            append("font-src 'self' data:; ")
            append("connect-src 'self' https://*.sentry.io; ") // Allow Sentry error reporting
            append("frame-ancestors 'none'; ") // Prevent framing
            append("base-uri 'self'; ") // Restrict base URI
            append("form-action 'self'; ") // Restrict form submissions
            append("upgrade-insecure-requests") // Force HTTPS upgrades
        }
    }
}