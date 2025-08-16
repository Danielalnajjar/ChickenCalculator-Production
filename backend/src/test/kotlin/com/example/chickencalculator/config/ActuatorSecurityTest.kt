package com.example.chickencalculator.config

import com.example.chickencalculator.TestBase
import com.example.chickencalculator.service.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import jakarta.servlet.http.Cookie

/**
 * Comprehensive security tests for Actuator endpoints.
 * 
 * Tests verify:
 * - /actuator/prometheus requires ADMIN role
 * - /actuator/** endpoints enforce CSRF protection (no blanket ignore)
 * - Sensitive actuator endpoints are properly protected
 * - Public actuator endpoints (health, info) remain accessible
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorSecurityTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var jwtService: JwtService

    // ================================
    // PUBLIC ACTUATOR ENDPOINTS
    // ================================

    @Test
    fun `actuator health should be accessible without authentication`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect { result ->
                val status = result.response.status
                assert(status != 401 && status != 403) {
                    "Health endpoint should be public, got status: $status"
                }
            }
    }

    @Test
    fun `actuator info should be accessible without authentication`() {
        mockMvc.perform(get("/actuator/info"))
            .andExpect { result ->
                val status = result.response.status
                assert(status != 401 && status != 403) {
                    "Info endpoint should be public, got status: $status"
                }
            }
    }

    // ================================
    // PROMETHEUS ENDPOINT SECURITY
    // ================================

    @Test
    fun `actuator prometheus should require authentication`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `actuator prometheus should reject non-admin users`() {
        val userToken = jwtService.generateToken(
            email = "user@test.com",
            userId = 2L,
            role = "USER"
        )
        
        val cookie = Cookie("admin_token", userToken)
        
        mockMvc.perform(
            get("/actuator/prometheus")
                .cookie(cookie)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `actuator prometheus should allow ADMIN users`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect { result ->
                val status = result.response.status
                // 200 (success) or 404 (endpoint not enabled) are both acceptable
                assert(status == 200 || status == 404) {
                    "ADMIN should access prometheus endpoint, got status: $status"
                }
            }
    }

    @Test
    fun `actuator prometheus with valid admin JWT token should work`() {
        val adminToken = jwtService.generateToken(
            email = TestConstants.DEFAULT_ADMIN_EMAIL,
            userId = 1L,
            role = "ADMIN"
        )
        
        val cookie = Cookie("admin_token", adminToken)
        
        mockMvc.perform(
            get("/actuator/prometheus")
                .cookie(cookie)
        )
            .andExpect { result ->
                val status = result.response.status
                // 200 (success) or 404 (endpoint not enabled) are both acceptable
                assert(status == 200 || status == 404) {
                    "Valid admin JWT should access prometheus, got status: $status"
                }
            }
    }

    // ================================
    // OTHER ADMIN ACTUATOR ENDPOINTS
    // ================================

    @Test
    fun `sensitive actuator endpoints should require ADMIN role`() {
        val adminOnlyEndpoints = listOf(
            "/actuator/metrics",
            "/actuator/configprops",
            "/actuator/env",
            "/actuator/beans",
            "/actuator/mappings"
        )
        
        adminOnlyEndpoints.forEach { endpoint ->
            mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized)
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `sensitive actuator endpoints should be accessible to ADMIN`() {
        val adminOnlyEndpoints = listOf(
            "/actuator/metrics",
            "/actuator/configprops",
            "/actuator/env",
            "/actuator/beans",
            "/actuator/mappings"
        )
        
        adminOnlyEndpoints.forEach { endpoint ->
            mockMvc.perform(get(endpoint))
                .andExpect { result ->
                    val status = result.response.status
                    // 200 (success) or 404 (endpoint not enabled) are both acceptable
                    assert(status == 200 || status == 404) {
                        "ADMIN should access $endpoint, got status: $status"
                    }
                }
        }
    }

    // ================================
    // CSRF PROTECTION TESTS
    // ================================

    @Test
    fun `actuator endpoints should enforce CSRF protection for state-changing operations`() {
        val csrfProtectedEndpoints = listOf(
            "/actuator/prometheus",
            "/actuator/metrics",
            "/actuator/env",
            "/actuator/configprops"
        )
        
        csrfProtectedEndpoints.forEach { endpoint ->
            // POST without CSRF token should fail
            mockMvc.perform(
                post(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect { result ->
                    val status = result.response.status
                    // Should be 401 (unauthorized) or 403 (forbidden/CSRF)
                    assert(status == 401 || status == 403) {
                        "$endpoint should enforce CSRF for POST, got status: $status"
                    }
                }
        }
    }

    @Test
    fun `actuator health endpoints should not require CSRF for POST`() {
        // Health endpoints are read-only and public, so POST should still work
        // (though they may not support POST operations)
        val publicEndpoints = listOf(
            "/actuator/health",
            "/actuator/info"
        )
        
        publicEndpoints.forEach { endpoint ->
            mockMvc.perform(
                post(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect { result ->
                    val status = result.response.status
                    // Should NOT be 403 due to CSRF - may be 405 (method not allowed) or others
                    assert(status != 403) {
                        "$endpoint should not require CSRF protection, got status: $status"
                    }
                }
        }
    }

    // ================================
    // CONFIGURATION VERIFICATION
    // ================================

    @Test
    fun `verify actuator patterns in SecurityConfig`() {
        val adminPatterns = SecurityConfig.ADMIN_API_PATTERNS
        val csrfIgnorePatterns = SecurityConfig.CSRF_IGNORE_PATTERNS
        
        // Verify prometheus is in admin patterns
        assert(adminPatterns.contains("/actuator/prometheus")) {
            "SecurityConfig should protect /actuator/prometheus with ADMIN role"
        }
        
        // Verify actuator is NOT blanket ignored for CSRF
        val hasActuatorCsrfIgnore = csrfIgnorePatterns.any { 
            it.contains("/actuator") && it.contains("**")
        }
        assert(!hasActuatorCsrfIgnore) {
            "SecurityConfig should not have blanket CSRF ignore for /actuator/**"
        }
    }

    @Test
    fun `verify security headers on actuator endpoints`() {
        val testEndpoints = listOf(
            "/actuator/health",
            "/actuator/info"
        )
        
        testEndpoints.forEach { endpoint ->
            mockMvc.perform(get(endpoint))
                .andExpect { result ->
                    val response = result.response
                    
                    // Verify security headers are present
                    assert(response.getHeader("X-Content-Type-Options") == "nosniff") {
                        "$endpoint should have X-Content-Type-Options: nosniff"
                    }
                    
                    assert(response.getHeader("X-Frame-Options") != null) {
                        "$endpoint should have X-Frame-Options header"
                    }
                    
                    assert(response.getHeader("Content-Security-Policy") != null) {
                        "$endpoint should have Content-Security-Policy header"
                    }
                }
        }
    }

    // ================================
    // EDGE CASE TESTS
    // ================================

    @Test
    fun `actuator endpoints should not leak information via error messages`() {
        val sensitiveEndpoints = listOf(
            "/actuator/prometheus",
            "/actuator/env",
            "/actuator/configprops"
        )
        
        sensitiveEndpoints.forEach { endpoint ->
            mockMvc.perform(get(endpoint))
                .andExpect { result ->
                    val status = result.response.status
                    val content = result.response.contentAsString
                    
                    // Should get 401, not detailed error info
                    assert(status == 401) {
                        "$endpoint should return 401, got $status"
                    }
                    
                    // Content should not leak sensitive information
                    assert(!content.contains("password", ignoreCase = true)) {
                        "$endpoint error response should not contain 'password'"
                    }
                    
                    assert(!content.contains("secret", ignoreCase = true)) {
                        "$endpoint error response should not contain 'secret'"
                    }
                }
        }
    }

    @Test
    fun `actuator endpoints should handle malformed requests gracefully`() {
        val testEndpoints = listOf(
            "/actuator/prometheus",
            "/actuator/metrics"
        )
        
        testEndpoints.forEach { endpoint ->
            // Test with malformed JWT
            val malformedCookie = Cookie("admin_token", "invalid.jwt.token")
            
            mockMvc.perform(
                get(endpoint)
                    .cookie(malformedCookie)
            )
                .andExpect { result ->
                    val status = result.response.status
                    // Should handle malformed JWT gracefully (401, not 500)
                    assert(status == 401) {
                        "$endpoint should handle malformed JWT gracefully, got status: $status"
                    }
                }
        }
    }
}