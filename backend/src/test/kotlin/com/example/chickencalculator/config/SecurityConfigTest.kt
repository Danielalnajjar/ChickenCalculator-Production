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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import jakarta.servlet.http.Cookie

/**
 * Test class for SecurityConfig to verify that public and protected paths
 * are correctly configured and prevent regression
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `public API endpoints should be accessible without authentication`() {
        val publicPaths = listOf(
            "/api/health",
            "/api/v1/admin/auth/login",
            "/api/v1/location/test/auth/login",
            "/api/v1/calculator/locations",
            "/actuator/health",
            "/actuator/info"
        )

        publicPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val status = result.response.status
                    // Public paths should not return 401/403
                    assert(status != 401 && status != 403) {
                        "Public path $path returned unauthorized status: $status"
                    }
                }
        }
    }

    @Test
    fun `static resources should be accessible without authentication`() {
        val staticPaths = listOf(
            "/",
            "/admin",
            "/admin/login",
            "/location/test",
            "/favicon.ico",
            "/manifest.json"
        )

        staticPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val status = result.response.status
                    // Static resources should not return 401/403
                    assert(status != 401 && status != 403) {
                        "Static path $path returned unauthorized status: $status"
                    }
                }
        }
    }

    @Test
    fun `admin API endpoints should require authentication`() {
        val protectedPaths = listOf(
            "/api/v1/admin/locations",
            "/api/v1/admin/locations/123",
            "/api/v1/admin/stats"
        )

        protectedPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun `CSRF should be disabled for auth endpoints`() {
        val csrfExemptPaths = listOf(
            "/api/v1/admin/auth/login",
            "/api/v1/location/test/auth/login",
            "/api/health"
        )

        csrfExemptPaths.forEach { path ->
            // POST without CSRF token should work for these paths
            mockMvc.perform(post(path)
                .contentType("application/json")
                .content("{}"))
                .andExpect { result ->
                    val status = result.response.status
                    // Should not get 403 Forbidden due to missing CSRF
                    assert(status != 403) {
                        "CSRF-exempt path $path returned 403: likely CSRF issue"
                    }
                }
        }
    }

    @Test
    fun `security headers should be present`() {
        mockMvc.perform(get("/api/health"))
            .andExpect { result ->
                val headers = result.response
                
                // Check for security headers
                assert(headers.getHeader("X-Content-Type-Options") == "nosniff") {
                    "X-Content-Type-Options header missing or incorrect"
                }
                
                assert(headers.getHeader("X-XSS-Protection") != null) {
                    "X-XSS-Protection header missing"
                }
                
                assert(headers.getHeader("X-Frame-Options") != null) {
                    "X-Frame-Options header missing"
                }
                
                assert(headers.getHeader("Content-Security-Policy") != null) {
                    "Content-Security-Policy header missing"
                }
            }
    }

    @Test
    fun `path patterns should not cause exceptions`() {
        // Test that our centralized patterns don't cause issues
        val testPaths = SecurityConfig.PUBLIC_API_PATTERNS.toList() +
                       SecurityConfig.PUBLIC_STATIC_PATTERNS.toList() +
                       SecurityConfig.ADMIN_API_PATTERNS.toList()

        testPaths.forEach { pattern ->
            // Remove wildcards for actual testing
            val testPath = pattern.replace("**", "test").replace("*", "test")
            
            mockMvc.perform(get(testPath))
                .andExpect { result ->
                    val status = result.response.status
                    // Should not cause 500 errors
                    assert(status != 500) {
                        "Pattern $pattern (tested as $testPath) caused a 500 error"
                    }
                }
        }
    }

    // ================================
    // ACTUATOR ENDPOINT SECURITY TESTS
    // ================================

    @Test
    fun `actuator health endpoints should be public`() {
        val publicActuatorPaths = listOf(
            "/actuator/health",
            "/actuator/info"
        )

        publicActuatorPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val status = result.response.status
                    // Public actuator paths should not return 401/403
                    assert(status != 401 && status != 403) {
                        "Public actuator path $path returned unauthorized status: $status"
                    }
                }
        }
    }

    @Test
    fun `actuator prometheus should require ADMIN role - unauthorized without token`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `actuator prometheus should require ADMIN role - forbidden with non-admin token`() {
        // Generate token for non-admin user
        val nonAdminToken = jwtService.generateToken(
            email = "user@test.com",
            userId = 2L,
            role = "USER"
        )
        
        val cookie = Cookie("admin_token", nonAdminToken)
        
        mockMvc.perform(
            get("/actuator/prometheus")
                .cookie(cookie)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `actuator prometheus should be accessible with ADMIN role`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect { result ->
                val status = result.response.status
                // Should be accessible with ADMIN role (200 or 404 if endpoint not configured)
                assert(status == 200 || status == 404) {
                    "ADMIN user should be able to access /actuator/prometheus, got status: $status"
                }
            }
    }

    @Test
    fun `actuator metrics should require ADMIN role`() {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `actuator metrics should be accessible with ADMIN role`() {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect { result ->
                val status = result.response.status
                // Should be accessible with ADMIN role (200 or 404 if endpoint not configured)
                assert(status == 200 || status == 404) {
                    "ADMIN user should be able to access /actuator/metrics, got status: $status"
                }
            }
    }

    @Test
    fun `actuator endpoints should enforce CSRF protection`() {
        val csrfProtectedActuatorPaths = listOf(
            "/actuator/prometheus",
            "/actuator/metrics",
            "/actuator/configprops",
            "/actuator/env"
        )

        csrfProtectedActuatorPaths.forEach { path ->
            // POST without CSRF token should fail for actuator endpoints
            mockMvc.perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect { result ->
                    val status = result.response.status
                    // Should get 401 (no auth) or 403 (missing CSRF) - NOT success
                    assert(status == 401 || status == 403) {
                        "Actuator path $path should require CSRF protection for POST, got status: $status"
                    }
                }
        }
    }

    @Test
    fun `actuator prometheus with valid admin JWT should work`() {
        // Generate valid admin token
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
                // Should be accessible with valid admin token (200 or 404 if endpoint not configured)
                assert(status == 200 || status == 404) {
                    "Valid admin token should allow access to /actuator/prometheus, got status: $status"
                }
            }
    }

    @Test
    fun `actuator endpoints should have security headers`() {
        val actuatorPaths = listOf(
            "/actuator/health",
            "/actuator/info"
        )

        actuatorPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val headers = result.response
                    
                    // Check for security headers on actuator endpoints
                    assert(headers.getHeader("X-Content-Type-Options") == "nosniff") {
                        "Actuator path $path missing X-Content-Type-Options header"
                    }
                    
                    assert(headers.getHeader("X-Frame-Options") != null) {
                        "Actuator path $path missing X-Frame-Options header"
                    }
                }
        }
    }

    @Test
    fun `actuator endpoints should not leak sensitive information without authentication`() {
        val sensitiveActuatorPaths = listOf(
            "/actuator/configprops",
            "/actuator/env",
            "/actuator/beans",
            "/actuator/mappings",
            "/actuator/prometheus",
            "/actuator/metrics"
        )

        sensitiveActuatorPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val status = result.response.status
                    // Sensitive actuator endpoints should require authentication
                    assert(status == 401 || status == 404) {
                        "Sensitive actuator path $path should require authentication, got status: $status"
                    }
                }
        }
    }

    @Test
    fun `verify actuator CSRF ignore patterns are removed`() {
        val csrfIgnorePatterns = SecurityConfig.CSRF_IGNORE_PATTERNS
        
        // Verify /actuator/** is NOT in CSRF ignore patterns
        val hasActuatorIgnore = csrfIgnorePatterns.any { pattern ->
            pattern.contains("/actuator") && pattern.contains("**")
        }
        
        assert(!hasActuatorIgnore) {
            "CSRF ignore patterns should not contain /actuator/** pattern. Found: ${csrfIgnorePatterns.joinToString()}"
        }
    }

    @Test
    fun `verify actuator prometheus is in admin patterns`() {
        val adminPatterns = SecurityConfig.ADMIN_API_PATTERNS
        
        // Verify /actuator/prometheus is protected by admin role
        val hasPrometheusAdmin = adminPatterns.any { pattern ->
            pattern.contains("/actuator/prometheus") || 
            (pattern.contains("/actuator") && pattern.contains("**"))
        }
        
        assert(hasPrometheusAdmin) {
            "Admin patterns should protect /actuator/prometheus. Found: ${adminPatterns.joinToString()}"
        }
    }
}