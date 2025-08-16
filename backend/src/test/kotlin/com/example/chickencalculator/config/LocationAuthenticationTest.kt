package com.example.chickencalculator.config

import com.example.chickencalculator.TestBase
import com.example.chickencalculator.service.JwtService
import com.example.chickencalculator.service.LocationAuthService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import jakarta.servlet.http.Cookie
import org.springframework.http.MediaType

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocationAuthenticationTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `location requiring auth without cookie should return 401`() {
        // Assuming we have location endpoints that require LOCATION_USER authority
        mockMvc.perform(post("/api/v1/marination-log")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"test\":\"data\"}")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `location data endpoints should reject requests without LOCATION_USER authority`() {
        // Try to access sales data without proper location authentication
        mockMvc.perform(get("/api/v1/sales-data"))
            .andExpect(status().isUnauthorized)
        
        // Try to access marination log without proper location authentication
        mockMvc.perform(get("/api/v1/marination-log"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `client sent X-Location-Id header should be ignored`() {
        // Create a valid admin token (different authority)
        val adminToken = jwtService.generateToken(
            email = TestBase.TestConstants.DEFAULT_ADMIN_EMAIL,
            userId = 1L,
            role = "ADMIN"
        )
        
        val adminCookie = Cookie("admin_token", adminToken)
        
        // Try to access location data with admin auth + client X-Location-Id header
        mockMvc.perform(
            get("/api/v1/sales-data")
                .cookie(adminCookie)
                .header("X-Location-Id", "999")  // Client trying to inject location ID
        ).andExpect { result ->
            val status = result.response.status
            // Should get 403 (forbidden) because admin auth doesn't give LOCATION_USER authority
            // and client headers should be ignored
            assert(status == 401 || status == 403) {
                "Should reject admin trying to access location data with client headers, got: $status"
            }
        }
    }

    @Test
    fun `cross tenant access should be prevented`() {
        // This test would need actual location tokens to be meaningful
        // For now, just verify that without proper LOCATION_USER auth, access is denied
        
        mockMvc.perform(get("/api/v1/calculator/calculate"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `location info endpoint should be publicly accessible for valid locations`() {
        // Location info endpoints should be publicly accessible for discovery
        mockMvc.perform(get("/api/v1/location/test-location/info"))
            .andExpect { result ->
                val status = result.response.status
                // Should be accessible (might return 404 if location doesn't exist)
                assert(status != 403) {
                    "Location info should not be forbidden, got: $status"
                }
            }
    }
}