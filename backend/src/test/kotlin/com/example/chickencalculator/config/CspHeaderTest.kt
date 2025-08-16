package com.example.chickencalculator.config

import com.example.chickencalculator.TestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CspHeaderTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `HTML pages should have CSP header with nonce`() {
        mockMvc.perform(get("/admin"))
            .andExpect { result ->
                val cspHeader = result.response.getHeader("Content-Security-Policy")
                assert(cspHeader != null) {
                    "CSP header should be present on HTML pages"
                }
                
                if (cspHeader != null) {
                    // Check that CSP contains nonce
                    assert(cspHeader.contains("nonce-")) {
                        "CSP should contain nonce directive, got: $cspHeader"
                    }
                    
                    // Check that unsafe-eval is NOT present
                    assert(!cspHeader.contains("unsafe-eval")) {
                        "CSP should not contain unsafe-eval, got: $cspHeader"
                    }
                    
                    // Check basic security directives
                    assert(cspHeader.contains("default-src 'self'")) {
                        "CSP should restrict default-src to self, got: $cspHeader"
                    }
                    
                    assert(cspHeader.contains("frame-ancestors 'none'")) {
                        "CSP should prevent framing, got: $cspHeader"
                    }
                    
                    // Check Sentry connection is allowed
                    assert(cspHeader.contains("connect-src 'self' https://*.sentry.io")) {
                        "CSP should allow Sentry connections, got: $cspHeader"
                    }
                }
            }
    }

    @Test
    fun `API endpoints should not have CSP headers`() {
        mockMvc.perform(get("/api/health"))
            .andExpect { result ->
                val cspHeader = result.response.getHeader("Content-Security-Policy")
                assert(cspHeader == null) {
                    "API endpoints should not have CSP headers, got: $cspHeader"
                }
            }
    }

    @Test
    fun `location info endpoint should not have CSP headers`() {
        mockMvc.perform(get("/api/v1/location/test-location/info"))
            .andExpect { result ->
                val cspHeader = result.response.getHeader("Content-Security-Policy")
                assert(cspHeader == null) {
                    "API endpoints should not have CSP headers, got: $cspHeader"
                }
            }
    }

    @Test
    fun `actuator endpoints should not have CSP headers`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect { result ->
                val cspHeader = result.response.getHeader("Content-Security-Policy")
                assert(cspHeader == null) {
                    "Actuator endpoints should not have CSP headers, got: $cspHeader"
                }
            }
    }

    @Test
    fun `static resource requests should not have CSP headers`() {
        // Test various static resource patterns
        val staticPaths = listOf(
            "/static/js/app.js",
            "/static/css/style.css",
            "/manifest.json",
            "/favicon.ico"
        )
        
        staticPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val cspHeader = result.response.getHeader("Content-Security-Policy")
                    assert(cspHeader == null) {
                        "Static resource $path should not have CSP header, got: $cspHeader"
                    }
                }
        }
    }

    @Test
    fun `CSP nonce should be different for each request`() {
        val nonces = mutableSetOf<String>()
        
        // Make multiple requests and extract nonces
        repeat(3) {
            mockMvc.perform(get("/admin"))
                .andExpect { result ->
                    val cspHeader = result.response.getHeader("Content-Security-Policy")
                    if (cspHeader != null) {
                        // Extract nonce from CSP header
                        val nonceRegex = Regex("nonce-([A-Za-z0-9+/=]+)")
                        val matchResult = nonceRegex.find(cspHeader)
                        if (matchResult != null) {
                            val nonce = matchResult.groupValues[1]
                            nonces.add(nonce)
                        }
                    }
                }
        }
        
        // All nonces should be different
        assert(nonces.size >= 2) {
            "Each request should generate a unique nonce, but got same nonces: $nonces"
        }
    }
}