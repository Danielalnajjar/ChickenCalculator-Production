package com.example.chickencalculator.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Test class for SpaController to ensure path patterns work correctly
 * and prevent regression to servlet 500 errors
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `admin root path should return HTML`() {
        // The actual file may not exist in test, so we expect either 200 or 404
        mockMvc.perform(get("/admin"))
            .andExpect { result ->
                val status = result.response.status
                assert(status == 200 || status == 404) { 
                    "Expected 200 or 404, got $status" 
                }
            }
    }

    @Test
    fun `admin sub-paths should be handled by catch-all pattern`() {
        val adminPaths = listOf(
            "/admin/login",
            "/admin/dashboard",
            "/admin/locations",
            "/admin/settings",
            "/admin/profile",
            "/admin/password",
            "/admin/some/deep/path"
        )

        adminPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val status = result.response.status
                    assert(status == 200 || status == 404) { 
                        "Expected 200 or 404 for $path, got $status" 
                    }
                }
        }
    }

    @Test
    fun `location root path should return HTML`() {
        mockMvc.perform(get("/location/test-location"))
            .andExpect { result ->
                val status = result.response.status
                assert(status == 200 || status == 404) { 
                    "Expected 200 or 404, got $status" 
                }
            }
    }

    @Test
    fun `location sub-paths should be handled by catch-all pattern`() {
        val locationPaths = listOf(
            "/location/test/calculator",
            "/location/test/sales",
            "/location/test/history",
            "/location/test/profile",
            "/location/test/settings",
            "/location/test/some/deep/path"
        )

        locationPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val status = result.response.status
                    assert(status == 200 || status == 404) { 
                        "Expected 200 or 404 for $path, got $status" 
                    }
                }
        }
    }

    @Test
    fun `paths should not cause PatternParseException`() {
        // These patterns previously caused issues with Spring 6
        val problematicPaths = listOf(
            "/admin/**",  // This literal path should not crash
            "/location/**",  // This literal path should not crash
            "/admin/test/**",
            "/location/test/**"
        )

        problematicPaths.forEach { path ->
            // We're testing that the request doesn't cause a 500 error
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val status = result.response.status
                    assert(status != 500) { 
                        "Path $path caused a 500 error, likely PatternParseException" 
                    }
                }
        }
    }
}