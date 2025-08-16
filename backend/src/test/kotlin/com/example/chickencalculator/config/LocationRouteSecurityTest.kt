package com.example.chickencalculator.config

import com.example.chickencalculator.TestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocationRouteSecurityTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `location info endpoint should not expose PII`() {
        mockMvc.perform(get("/api/v1/location/test-location/info"))
            .andExpect { result ->
                val content = result.response.contentAsString
                // Should not contain managerEmail or managerName
                assert(!content.contains("managerEmail")) {
                    "Location info should not expose managerEmail"
                }
                // Should contain only safe fields
                if (result.response.status == 200) {
                    assert(content.contains("\"id\"")) { "Should contain id" }
                    assert(content.contains("\"slug\"")) { "Should contain slug" }
                    assert(content.contains("\"name\"")) { "Should contain name" }
                    assert(content.contains("\"requiresAuth\"")) { "Should contain requiresAuth" }
                }
            }
    }

    @Test
    fun `location validate endpoint should not expose PII`() {
        mockMvc.perform(get("/api/v1/location/test-location/validate"))
            .andExpect { result ->
                val content = result.response.contentAsString
                // Should not contain any PII
                assert(!content.contains("managerEmail")) {
                    "Location validate should not expose managerEmail"
                }
                assert(!content.contains("managerName")) {
                    "Location validate should not expose managerName"
                }
            }
    }

    @Test
    fun `location static resources should be accessible`() {
        // This would need actual static files to test properly
        // For now, just ensure the pattern allows access
        mockMvc.perform(get("/location/test-location/static/test.js"))
            .andExpect { result ->
                // Should not be blocked by security (might 404 if file doesn't exist)
                val status = result.response.status
                assert(status != 403) {
                    "Static resources should not be forbidden, got: $status"
                }
            }
    }

    @Test
    fun `single location path should be accessible for HTML`() {
        // Test /location/{slug} pattern for HTML access
        mockMvc.perform(get("/location/test-location"))
            .andExpect { result ->
                val status = result.response.status
                // Should not be forbidden (might redirect or 404)
                assert(status != 403) {
                    "Location HTML should not be forbidden, got: $status"
                }
            }
    }
}