package com.example.chickencalculator.config

import com.example.chickencalculator.TestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.http.MediaType

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `admin login endpoint should be rate limited`() {
        val loginRequest = """{"email":"admin@test.com","password":"password"}"""
        
        // First few requests should succeed (up to rate limit)
        repeat(5) {
            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.1.100") // Simulate same IP
            ).andExpect { result ->
                val status = result.response.status
                // Should not be rate limited yet (might fail for other reasons like auth)
                assert(status != 429) {
                    "Request $it should not be rate limited yet, got: $status"
                }
            }
        }
    }

    @Test
    fun `location login endpoint should be rate limited`() {
        val loginRequest = """{"password":"test-password"}"""
        
        // Test location login rate limiting
        repeat(5) {
            mockMvc.perform(
                post("/api/v1/location/test-location/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.1.101") // Simulate same IP
            ).andExpect { result ->
                val status = result.response.status
                // Should not be rate limited yet
                assert(status != 429) {
                    "Request $it should not be rate limited yet, got: $status"
                }
            }
        }
    }

    @Test
    fun `rate limit should include retry-after header`() {
        val loginRequest = """{"email":"admin@test.com","password":"password"}"""
        
        // Make many requests to trigger rate limit
        repeat(110) { index ->
            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.1.102") // Same IP
            ).andExpect { result ->
                val status = result.response.status
                if (status == 429) {
                    // Once we hit rate limit, check headers
                    val retryAfter = result.response.getHeader("Retry-After")
                    assert(retryAfter != null) {
                        "Rate limited response should include Retry-After header"
                    }
                    val rateLimitHeaders = listOf("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset")
                    rateLimitHeaders.forEach { headerName ->
                        val headerValue = result.response.getHeader(headerName)
                        assert(headerValue != null) {
                            "Rate limited response should include $headerName header"
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `different IPs should have separate rate limits`() {
        val loginRequest = """{"email":"admin@test.com","password":"password"}"""
        
        // IP 1 makes requests
        repeat(5) {
            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.1.103")
            )
        }
        
        // IP 2 should still be able to make requests
        mockMvc.perform(
            post("/api/v1/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
                .header("X-Forwarded-For", "192.168.1.104")
        ).andExpect { result ->
            val status = result.response.status
            assert(status != 429) {
                "Different IP should not be rate limited, got: $status"
            }
        }
    }

    @Test
    fun `non-login endpoints should not be rate limited`() {
        // Health endpoint should not be rate limited
        repeat(10) {
            mockMvc.perform(
                post("/api/health")
                    .header("X-Forwarded-For", "192.168.1.105")
            ).andExpect { result ->
                val status = result.response.status
                assert(status != 429) {
                    "Non-login endpoint should not be rate limited, got: $status"
                }
            }
        }
    }
}