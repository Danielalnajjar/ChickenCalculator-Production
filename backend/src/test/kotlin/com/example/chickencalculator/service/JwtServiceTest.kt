package com.example.chickencalculator.service

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setup() {
        // Set JWT_SECRET for testing
        System.setProperty("jwt.secret", "TestSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm")
        jwtService = JwtService()
    }

    @Nested
    @DisplayName("Token Generation Tests")
    inner class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token")
        fun testGenerateToken() {
            // Given
            val email = "test@example.com"
            val userId = 123L
            val role = "ADMIN"

            // When
            val token = jwtService.generateToken(email, userId, role)

            // Then
            assertNotNull(token)
            assertTrue(token.isNotEmpty())
            assertTrue(token.split(".").size == 3) // JWT has 3 parts
        }

        @Test
        @DisplayName("Should include claims in token")
        fun testTokenClaims() {
            // Given
            val email = "test@example.com"
            val userId = 456L
            val role = "USER"

            // When
            val token = jwtService.generateToken(email, userId, role)

            // Then
            assertEquals(email, jwtService.getEmailFromToken(token))
            assertEquals(userId, jwtService.getUserIdFromToken(token))
            assertEquals(role, jwtService.getRoleFromToken(token))
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        fun testUniqueTokens() {
            // When
            val token1 = jwtService.generateToken("user1@test.com", 1L, "USER")
            val token2 = jwtService.generateToken("user2@test.com", 2L, "ADMIN")

            // Then
            assertNotEquals(token1, token2)
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    inner class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token")
        fun testValidateCorrectToken() {
            // Given
            val token = jwtService.generateToken("test@example.com", 1L, "ADMIN")

            // When
            val isValid = jwtService.validateToken(token)

            // Then
            assertTrue(isValid)
        }

        @Test
        @DisplayName("Should reject invalid token")
        fun testValidateInvalidToken() {
            // Given
            val invalidToken = "invalid.jwt.token"

            // When
            val isValid = jwtService.validateToken(invalidToken)

            // Then
            assertFalse(isValid)
        }

        @Test
        @DisplayName("Should reject tampered token")
        fun testValidateTamperedToken() {
            // Given
            val token = jwtService.generateToken("test@example.com", 1L, "ADMIN")
            val tamperedToken = token.substring(0, token.length - 5) + "XXXXX"

            // When
            val isValid = jwtService.validateToken(tamperedToken)

            // Then
            assertFalse(isValid)
        }

        @Test
        @DisplayName("Should reject empty token")
        fun testValidateEmptyToken() {
            // When
            val isValid = jwtService.validateToken("")

            // Then
            assertFalse(isValid)
        }

        @Test
        @DisplayName("Should handle malformed token gracefully")
        fun testValidateMalformedToken() {
            // Given
            val malformedToken = "not.a.jwt"

            // When
            val isValid = jwtService.validateToken(malformedToken)

            // Then
            assertFalse(isValid)
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    inner class TokenExtractionTests {

        @Test
        @DisplayName("Should extract email from valid token")
        fun testExtractEmail() {
            // Given
            val email = "user@example.com"
            val token = jwtService.generateToken(email, 1L, "USER")

            // When
            val extractedEmail = jwtService.getEmailFromToken(token)

            // Then
            assertEquals(email, extractedEmail)
        }

        @Test
        @DisplayName("Should extract userId from valid token")
        fun testExtractUserId() {
            // Given
            val userId = 789L
            val token = jwtService.generateToken("test@test.com", userId, "ADMIN")

            // When
            val extractedUserId = jwtService.getUserIdFromToken(token)

            // Then
            assertEquals(userId, extractedUserId)
        }

        @Test
        @DisplayName("Should extract role from valid token")
        fun testExtractRole() {
            // Given
            val role = "SUPER_ADMIN"
            val token = jwtService.generateToken("admin@test.com", 1L, role)

            // When
            val extractedRole = jwtService.getRoleFromToken(token)

            // Then
            assertEquals(role, extractedRole)
        }

        @Test
        @DisplayName("Should return null for invalid token extraction")
        fun testExtractFromInvalidToken() {
            // Given
            val invalidToken = "invalid.token.here"

            // When
            val email = jwtService.getEmailFromToken(invalidToken)
            val userId = jwtService.getUserIdFromToken(invalidToken)
            val role = jwtService.getRoleFromToken(invalidToken)

            // Then
            assertNull(email)
            assertNull(userId)
            assertNull(role)
        }
    }

    @Nested
    @DisplayName("Token Security Tests")
    inner class TokenSecurityTests {

        @Test
        @DisplayName("Should use consistent signing key")
        fun testConsistentSigningKey() {
            // Generate token with one instance
            val token1 = jwtService.generateToken("test@test.com", 1L, "USER")
            
            // Create new instance (simulating restart)
            val newJwtService = JwtService()
            
            // Should be able to validate token from previous instance
            assertTrue(newJwtService.validateToken(token1))
            assertEquals("test@test.com", newJwtService.getEmailFromToken(token1))
        }

        @Test
        @DisplayName("Should not accept tokens with different signing key")
        fun testDifferentSigningKey() {
            // Generate token with current service
            val token = jwtService.generateToken("test@test.com", 1L, "USER")
            
            // Create new service with different key
            System.setProperty("jwt.secret", "DifferentSecretKeyForTestingPurposesOnly12345678")
            val differentKeyService = JwtService()
            
            // Should not validate token signed with different key
            assertFalse(differentKeyService.validateToken(token))
        }
    }
}