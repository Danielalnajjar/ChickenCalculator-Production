package com.example.chickencalculator.security

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.web.util.pattern.PathPatternParser

class PathPatternSanityTest {

    private val validPatterns = listOf(
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus",
        "/location/*",
        "/location/*/static/**",
        "/api/v1/sales-data/**",
        "/api/v1/marination-log/**"
    )

    @Test
    fun `all configured patterns parse under Spring 6 PathPatternParser`() {
        val parser = PathPatternParser()
        validPatterns.forEach { p ->
            assertDoesNotThrow({ parser.parse(p) }, "Failed to parse pattern: $p")
        }
    }
}