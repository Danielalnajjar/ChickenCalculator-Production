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
import jakarta.servlet.http.Cookie

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorSecurityTest : TestBase() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `actuator health should be publicly accessible`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `actuator prometheus should require admin authentication`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect { result ->
                val status = result.response.status
                assert(status == 401 || status == 403) {
                    "Prometheus endpoint should require authentication, got: $status"
                }
            }
    }

    @Test
    fun `actuator loggers should not be exposed`() {
        mockMvc.perform(get("/actuator/loggers"))
            .andExpect(status().isNotFound)
    }
}