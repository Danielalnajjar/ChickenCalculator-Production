package com.example.chickencalculator.config

import com.example.chickencalculator.TestBase
import com.example.chickencalculator.service.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import jakarta.servlet.http.Cookie

/**
 * Specific tests for Prometheus metrics endpoint security
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrometheusSecurityTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `prometheus endpoint should require admin role`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect { result ->
                val status = result.response.status
                assert(status == 401 || status == 403) {
                    "Prometheus should require authentication, got: $status"
                }
            }
    }

    @Test
    fun `prometheus endpoint should work with valid admin JWT`() {
        val adminToken = jwtService.generateToken(
            email = TestBase.TestConstants.DEFAULT_ADMIN_EMAIL,
            userId = 1L,
            role = "ADMIN"
        )
        
        val cookie = Cookie("admin_token", adminToken)
        
        mockMvc.perform(
            get("/actuator/prometheus")
                .cookie(cookie)
        ).andExpect(status().isOk)
            .andExpect { result ->
                val content = result.response.contentAsString
                // Should contain Prometheus metrics format
                assert(content.contains("# HELP") || content.contains("# TYPE")) {
                    "Prometheus endpoint should return metrics in Prometheus format"
                }
            }
    }

    @Test
    fun `prometheus endpoint should reject non-admin users`() {
        // Create a token without ADMIN role
        val userToken = jwtService.generateToken(
            email = "user@test.com",
            userId = 2L,
            role = "USER"
        )
        
        val cookie = Cookie("admin_token", userToken)
        
        mockMvc.perform(
            get("/actuator/prometheus")
                .cookie(cookie)
        ).andExpect { result ->
            val status = result.response.status
            assert(status == 403) {
                "Non-admin should get 403 on Prometheus endpoint, got: $status"
            }
        }
    }

    @Test
    fun `prometheus endpoint should reject malformed JWT`() {
        val malformedToken = "invalid.jwt.token"
        val cookie = Cookie("admin_token", malformedToken)
        
        mockMvc.perform(
            get("/actuator/prometheus")
                .cookie(cookie)
        ).andExpect { result ->
            val status = result.response.status
            assert(status == 401) {
                "Malformed JWT should get 401, got: $status"
            }
        }
    }
}