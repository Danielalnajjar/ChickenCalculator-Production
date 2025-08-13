package com.example.chickencalculator.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Test class for SecurityConfig to verify that public and protected paths
 * are correctly configured and prevent regression
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

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
}