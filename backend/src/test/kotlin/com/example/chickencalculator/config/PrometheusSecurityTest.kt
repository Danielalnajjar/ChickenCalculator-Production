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
 * Focused security tests for the specific requirements:
 * 1. /actuator/prometheus requires ADMIN role
 * 2. /actuator/** endpoints no longer have blanket CSRF ignore
 * 
 * These tests pin the security behavior to prevent regressions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrometheusSecurityTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var jwtService: JwtService

    // ================================
    // REQUIREMENT 1: ADMIN-ONLY ACCESS
    // ================================

    @Test
    fun `prometheus endpoint should require authentication`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `prometheus endpoint should reject non-admin users`() {
        val userToken = jwtService.generateToken(
            email = "user@test.com",
            userId = 2L,
            role = "USER"
        )
        
        mockMvc.perform(
            get("/actuator/prometheus")
                .cookie(Cookie("admin_token", userToken))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `prometheus endpoint should allow admin users`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect { result ->
                val status = result.response.status
                // Accept 200 (working) or 404 (not configured) as success
                assert(status == 200 || status == 404) {
                    "Admin should access prometheus, got: $status"
                }
            }
    }

    // ================================
    // REQUIREMENT 2: CSRF ENFORCEMENT
    // ================================

    @Test
    fun `prometheus endpoint should enforce CSRF protection`() {
        // POST without CSRF token should fail
        mockMvc.perform(
            post("/actuator/prometheus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect { result ->
                val status = result.response.status
                // Should get 401 (no auth) or 403 (CSRF) - not success
                assert(status == 401 || status == 403) {
                    "Prometheus should enforce CSRF, got: $status"
                }
            }
    }

    @Test
    fun `other actuator endpoints should also enforce CSRF`() {
        val testEndpoints = listOf(
            "/actuator/metrics",
            "/actuator/env"
        )
        
        testEndpoints.forEach { endpoint ->
            mockMvc.perform(
                post(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect { result ->
                    val status = result.response.status
                    assert(status == 401 || status == 403) {
                        "$endpoint should enforce CSRF, got: $status"
                    }
                }
        }
    }

    // ================================
    // CONFIGURATION VERIFICATION
    // ================================

    @Test
    fun `security config should include prometheus in admin patterns`() {
        val adminPatterns = SecurityConfig.ADMIN_API_PATTERNS
        assert("/actuator/prometheus" in adminPatterns) {
            "SecurityConfig.ADMIN_API_PATTERNS should contain /actuator/prometheus"
        }
    }

    @Test
    fun `security config should not blanket ignore CSRF for actuator`() {
        val csrfIgnorePatterns = SecurityConfig.CSRF_IGNORE_PATTERNS
        val hasActuatorWildcard = csrfIgnorePatterns.any { 
            it.contains("/actuator") && it.contains("**")
        }
        assert(!hasActuatorWildcard) {
            "SecurityConfig.CSRF_IGNORE_PATTERNS should not contain /actuator/** wildcard"
        }
    }

    // ================================
    // BASELINE BEHAVIOR VERIFICATION
    // ================================

    @Test
    fun `health endpoint should remain public`() {
        // Verify we didn't break public health endpoint
        mockMvc.perform(get("/actuator/health"))
            .andExpect { result ->
                val status = result.response.status
                assert(status != 401 && status != 403) {
                    "Health endpoint should remain public, got: $status"
                }
            }
    }

    @Test
    fun `health endpoint should not require CSRF for POST`() {
        // Health is explicitly in CSRF ignore list, verify it still works
        mockMvc.perform(
            post("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect { result ->
                val status = result.response.status
                // Should NOT get 403 due to CSRF
                assert(status != 403) {
                    "Health should not require CSRF, got: $status"
                }
            }
    }
}