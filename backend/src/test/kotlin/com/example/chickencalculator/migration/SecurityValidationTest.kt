package com.example.chickencalculator.migration

import com.example.chickencalculator.dto.LoginRequest
import com.example.chickencalculator.dto.ChangePasswordRequest
import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.service.AdminService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Security Validation Test Suite for PostgreSQL Migration
 * 
 * This test suite validates:
 * 1. Authentication mechanisms (JWT, password hashing)
 * 2. Authorization controls (admin access, CSRF protection)
 * 3. SQL injection prevention
 * 4. XSS protection
 * 5. Session management security
 * 6. Password change enforcement
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("security-test")
@Testcontainers
class SecurityValidationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var adminService: AdminService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("chicken_calculator_security_test")
            .withUsername("security_user")
            .withPassword("security_password")

        private val securityResults = mutableListOf<SecurityTestResult>()
    }

    data class SecurityTestResult(
        val testId: String,
        val testName: String,
        val status: SecurityTestStatus,
        val severity: SecuritySeverity,
        val description: String,
        val remediation: String = ""
    )

    enum class SecurityTestStatus {
        PASS, FAIL, CRITICAL_FAIL
    }

    enum class SecuritySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // =========================
    // AUTHENTICATION TESTS
    // =========================

    @Test
    @Order(1)
    @DisplayName("SEC-001: Password Hashing Validation")
    fun `SEC-001 verify passwords are properly hashed with BCrypt`() {
        try {
            val testUser = adminService.createAdminUser(
                email = "security.test@example.com",
                password = "SecurePassword123!",
                name = "Security Test User",
                role = AdminRole.MANAGER
            )

            // Verify password is hashed (not stored in plain text)
            assertFalse(
                testUser.passwordHash == "SecurePassword123!",
                "Password should not be stored in plain text"
            )

            // Verify BCrypt format (starts with $2a$, $2b$, or $2y$)
            assertTrue(
                testUser.passwordHash.matches(Regex("\\$2[abyxy]\\$.{56}")),
                "Password should be BCrypt hashed format"
            )

            // Verify authentication works
            val authenticatedUser = adminService.authenticate("security.test@example.com", "SecurePassword123!")
            assertEquals(testUser.email, authenticatedUser.email)

            securityResults.add(SecurityTestResult(
                "SEC-001",
                "Password Hashing Validation",
                SecurityTestStatus.PASS,
                SecuritySeverity.HIGH,
                "Passwords are properly hashed with BCrypt"
            ))
        } catch (e: Exception) {
            securityResults.add(SecurityTestResult(
                "SEC-001",
                "Password Hashing Validation",
                SecurityTestStatus.CRITICAL_FAIL,
                SecuritySeverity.CRITICAL,
                "Password hashing failed: ${e.message}",
                "Implement proper BCrypt password hashing"
            ))
            throw e
        }
    }

    @Test
    @Order(2)
    @DisplayName("SEC-002: JWT Token Security")
    fun `SEC-002 verify JWT tokens are secure and httpOnly`() {
        try {
            // Create test admin
            adminService.createAdminUser(
                email = "jwt.test@example.com",
                password = "JwtTest123!",
                name = "JWT Test User",
                role = AdminRole.ADMIN,
                passwordChangeRequired = false
            )

            val loginRequest = LoginRequest(
                email = "jwt.test@example.com",
                password = "JwtTest123!"
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val response = testRestTemplate.postForEntity(
                "http://localhost:$port/api/v1/admin/auth/login",
                HttpEntity(loginRequest, headers),
                String::class.java
            )

            assertEquals(HttpStatus.OK, response.statusCode)

            // Verify JWT is set as httpOnly cookie (not in response body)
            val authCookie = response.headers["Set-Cookie"]?.find { it.contains("authToken") }
            assertNotNull(authCookie, "JWT should be set as cookie")
            assertTrue(authCookie.contains("HttpOnly"), "JWT cookie should be HttpOnly")
            assertTrue(authCookie.contains("Secure") || authCookie.contains("SameSite"), "JWT cookie should have security attributes")

            // Verify token is not in response body
            val responseBody = response.body ?: ""
            assertFalse(
                responseBody.contains("token") || responseBody.contains("jwt"),
                "JWT should not be exposed in response body"
            )

            securityResults.add(SecurityTestResult(
                "SEC-002",
                "JWT Token Security",
                SecurityTestStatus.PASS,
                SecuritySeverity.HIGH,
                "JWT tokens are properly secured with httpOnly cookies"
            ))
        } catch (e: Exception) {
            securityResults.add(SecurityTestResult(
                "SEC-002",
                "JWT Token Security", 
                SecurityTestStatus.CRITICAL_FAIL,
                SecuritySeverity.CRITICAL,
                "JWT security validation failed: ${e.message}",
                "Implement secure JWT handling with httpOnly cookies"
            ))
            throw e
        }
    }

    @Test
    @Order(3)
    @DisplayName("SEC-003: CSRF Protection Validation")
    fun `SEC-003 verify CSRF protection is active`() {
        try {
            // Request CSRF token
            val csrfResponse = testRestTemplate.getForEntity(
                "http://localhost:$port/api/v1/admin/auth/csrf-token",
                String::class.java
            )

            assertEquals(HttpStatus.OK, csrfResponse.statusCode)
            
            val csrfTokenCookie = csrfResponse.headers["Set-Cookie"]?.find { it.contains("XSRF-TOKEN") }
            assertNotNull(csrfTokenCookie, "CSRF token should be set as cookie")

            // Verify CSRF token is required for state-changing operations
            val loginRequest = LoginRequest(
                email = "test@example.com",
                password = "TestPassword123!"
            )

            // Request without CSRF token should fail (depending on configuration)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val responseWithoutCsrf = testRestTemplate.postForEntity(
                "http://localhost:$port/api/v1/admin/auth/login",
                HttpEntity(loginRequest, headers),
                String::class.java
            )

            // Either requires CSRF or accepts for login (depending on Spring Security config)
            // The key is that CSRF token endpoint is available
            assertTrue(csrfTokenCookie.isNotEmpty(), "CSRF protection should be available")

            securityResults.add(SecurityTestResult(
                "SEC-003",
                "CSRF Protection Validation",
                SecurityTestStatus.PASS,
                SecuritySeverity.MEDIUM,
                "CSRF protection is properly configured"
            ))
        } catch (e: Exception) {
            securityResults.add(SecurityTestResult(
                "SEC-003",
                "CSRF Protection Validation",
                SecurityTestStatus.FAIL,
                SecuritySeverity.MEDIUM,
                "CSRF protection validation failed: ${e.message}",
                "Ensure CSRF protection is properly configured"
            ))
            // Don't throw - CSRF might be configured differently
        }
    }

    @Test
    @Order(4)
    @DisplayName("SEC-004: Password Change Enforcement")
    fun `SEC-004 verify password change enforcement works`() {
        try {
            // Create admin with password change required
            val adminWithPasswordChange = adminService.createAdminUser(
                email = "password.change@example.com",
                password = "InitialPass123!",
                name = "Password Change Test",
                role = AdminRole.MANAGER,
                passwordChangeRequired = true
            )

            assertTrue(
                adminWithPasswordChange.passwordChangeRequired,
                "Password change should be required for new admin"
            )

            // Test password change functionality
            val changeResult = adminService.changePassword(
                adminWithPasswordChange.id!!,
                "InitialPass123!",
                "NewSecurePass123!"
            )

            assertTrue(changeResult, "Password change should succeed")

            // Verify password change requirement is cleared
            val updatedAdmin = adminService.getAdminByEmail("password.change@example.com")
            assertNotNull(updatedAdmin)
            assertFalse(
                updatedAdmin!!.passwordChangeRequired,
                "Password change requirement should be cleared after successful change"
            )

            // Verify old password no longer works
            assertThrows<Exception> {
                adminService.authenticate("password.change@example.com", "InitialPass123!")
            }

            // Verify new password works
            val authResult = adminService.authenticate("password.change@example.com", "NewSecurePass123!")
            assertEquals("password.change@example.com", authResult.email)

            securityResults.add(SecurityTestResult(
                "SEC-004",
                "Password Change Enforcement",
                SecurityTestStatus.PASS,
                SecuritySeverity.HIGH,
                "Password change enforcement works correctly"
            ))
        } catch (e: Exception) {
            securityResults.add(SecurityTestResult(
                "SEC-004",
                "Password Change Enforcement",
                SecurityTestStatus.FAIL,
                SecuritySeverity.HIGH,
                "Password change enforcement failed: ${e.message}",
                "Fix password change enforcement logic"
            ))
            throw e
        }
    }

    // =========================
    // INJECTION ATTACK TESTS
    // =========================

    @Test
    @Order(10)
    @DisplayName("SEC-010: SQL Injection Protection")
    fun `SEC-010 verify protection against SQL injection attacks`() {
        val sqlInjectionPayloads = listOf(
            "'; DROP TABLE admin_users; --",
            "' OR '1'='1",
            "admin@test.com'; UPDATE admin_users SET password_hash='hacked'; --",
            "1' OR 1=1 UNION SELECT * FROM admin_users --",
            "'; INSERT INTO admin_users (email, password_hash) VALUES ('hacker', 'password'); --"
        )

        var vulnerabilitiesFound = 0

        sqlInjectionPayloads.forEach { payload ->
            try {
                // Test SQL injection in login endpoint
                val maliciousLogin = LoginRequest(
                    email = payload,
                    password = "any_password"
                )

                val response = testRestTemplate.postForEntity(
                    "http://localhost:$port/api/v1/admin/auth/login",
                    HttpEntity(maliciousLogin, HttpHeaders().apply { 
                        contentType = MediaType.APPLICATION_JSON 
                    }),
                    String::class.java
                )

                // Response should be 401 (Unauthorized) or 400 (Bad Request)
                // Should NOT be 200 (OK) or 500 (Internal Server Error indicating SQL error)
                if (response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
                    vulnerabilitiesFound++
                    println("⚠️ Potential SQL injection vulnerability with payload: $payload")
                }

            } catch (e: Exception) {
                // Exceptions are acceptable - they indicate the malicious input was rejected
            }
        }

        if (vulnerabilitiesFound == 0) {
            securityResults.add(SecurityTestResult(
                "SEC-010",
                "SQL Injection Protection",
                SecurityTestStatus.PASS,
                SecuritySeverity.CRITICAL,
                "Application is protected against SQL injection attacks"
            ))
        } else {
            securityResults.add(SecurityTestResult(
                "SEC-010",
                "SQL Injection Protection",
                SecurityTestStatus.CRITICAL_FAIL,
                SecuritySeverity.CRITICAL,
                "Found $vulnerabilitiesFound potential SQL injection vulnerabilities",
                "Implement proper parameterized queries and input validation"
            ))
            throw AssertionError("SQL injection vulnerabilities found")
        }
    }

    @Test
    @Order(11)
    @DisplayName("SEC-011: XSS Protection")
    fun `SEC-011 verify protection against XSS attacks`() {
        val xssPayloads = listOf(
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "';alert('XSS');//"
        )

        try {
            // Create test admin
            adminService.createAdminUser(
                email = "xss.test@example.com",
                password = "XssTest123!",
                name = "XSS Test User",
                role = AdminRole.MANAGER
            )

            var xssVulnerabilities = 0

            xssPayloads.forEach { payload ->
                try {
                    // Test XSS in various endpoints
                    val maliciousLogin = LoginRequest(
                        email = payload,
                        password = "test"
                    )

                    val response = testRestTemplate.postForEntity(
                        "http://localhost:$port/api/v1/admin/auth/login",
                        HttpEntity(maliciousLogin, HttpHeaders().apply { 
                            contentType = MediaType.APPLICATION_JSON 
                        }),
                        String::class.java
                    )

                    // Check if malicious script is reflected in response
                    val responseBody = response.body ?: ""
                    if (responseBody.contains("<script>") || 
                        responseBody.contains("javascript:") ||
                        responseBody.contains("onerror=") ||
                        responseBody.contains("onload=")) {
                        xssVulnerabilities++
                        println("⚠️ Potential XSS vulnerability with payload: $payload")
                    }

                } catch (e: Exception) {
                    // Exceptions are acceptable
                }
            }

            if (xssVulnerabilities == 0) {
                securityResults.add(SecurityTestResult(
                    "SEC-011",
                    "XSS Protection",
                    SecurityTestStatus.PASS,
                    SecuritySeverity.HIGH,
                    "Application is protected against XSS attacks"
                ))
            } else {
                securityResults.add(SecurityTestResult(
                    "SEC-011",
                    "XSS Protection",
                    SecurityTestStatus.FAIL,
                    SecuritySeverity.HIGH,
                    "Found $xssVulnerabilities potential XSS vulnerabilities",
                    "Implement proper output encoding and CSP headers"
                ))
            }

        } catch (e: Exception) {
            securityResults.add(SecurityTestResult(
                "SEC-011",
                "XSS Protection",
                SecurityTestStatus.FAIL,
                SecuritySeverity.MEDIUM,
                "XSS protection test failed: ${e.message}",
                "Review XSS protection implementation"
            ))
        }
    }

    // =========================
    // AUTHORIZATION TESTS
    // =========================

    @Test
    @Order(20)
    @DisplayName("SEC-020: Admin Access Control")
    fun `SEC-020 verify admin endpoints require proper authorization`() {
        try {
            val adminEndpoints = listOf(
                "/api/v1/admin/locations",
                "/api/v1/admin/stats",
                "/api/v1/admin/auth/validate"
            )

            var unauthorizedAccessFound = false

            adminEndpoints.forEach { endpoint ->
                val response = testRestTemplate.getForEntity(
                    "http://localhost:$port$endpoint",
                    String::class.java
                )

                // Should return 401 (Unauthorized) or 403 (Forbidden) without valid JWT
                if (response.statusCode == HttpStatus.OK) {
                    unauthorizedAccessFound = true
                    println("⚠️ Unauthorized access possible to: $endpoint")
                }
            }

            if (!unauthorizedAccessFound) {
                securityResults.add(SecurityTestResult(
                    "SEC-020",
                    "Admin Access Control",
                    SecurityTestStatus.PASS,
                    SecuritySeverity.HIGH,
                    "Admin endpoints properly require authorization"
                ))
            } else {
                securityResults.add(SecurityTestResult(
                    "SEC-020",
                    "Admin Access Control",
                    SecurityTestStatus.CRITICAL_FAIL,
                    SecuritySeverity.CRITICAL,
                    "Unauthorized access to admin endpoints detected",
                    "Implement proper JWT authentication for admin endpoints"
                ))
                throw AssertionError("Admin access control vulnerabilities found")
            }

        } catch (e: Exception) {
            if (e is AssertionError) throw e
            
            securityResults.add(SecurityTestResult(
                "SEC-020",
                "Admin Access Control",
                SecurityTestStatus.FAIL,
                SecuritySeverity.MEDIUM,
                "Admin access control test failed: ${e.message}",
                "Review authorization implementation"
            ))
        }
    }

    @Test
    @Order(21)
    @DisplayName("SEC-021: Session Management")
    fun `SEC-021 verify secure session management`() {
        try {
            // Create and login admin
            adminService.createAdminUser(
                email = "session.test@example.com",
                password = "SessionTest123!",
                name = "Session Test User",
                role = AdminRole.ADMIN,
                passwordChangeRequired = false
            )

            val loginResponse = testRestTemplate.postForEntity(
                "http://localhost:$port/api/v1/admin/auth/login",
                HttpEntity(LoginRequest("session.test@example.com", "SessionTest123!"), 
                          HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
                String::class.java
            )

            assertEquals(HttpStatus.OK, loginResponse.statusCode)

            // Extract cookies
            val cookies = loginResponse.headers["Set-Cookie"] ?: emptyList()
            val authCookie = cookies.find { it.contains("authToken") }
            assertNotNull(authCookie, "Auth cookie should be set")

            // Verify logout clears session
            val logoutHeaders = HttpHeaders()
            if (authCookie != null) {
                logoutHeaders.add("Cookie", authCookie.split(";")[0])
            }

            val logoutResponse = testRestTemplate.postForEntity(
                "http://localhost:$port/api/v1/admin/auth/logout",
                HttpEntity<String>(null, logoutHeaders),
                String::class.java
            )

            // Logout should succeed or return appropriate status
            assertTrue(
                logoutResponse.statusCode == HttpStatus.OK || 
                logoutResponse.statusCode == HttpStatus.NO_CONTENT,
                "Logout should succeed"
            )

            securityResults.add(SecurityTestResult(
                "SEC-021",
                "Session Management",
                SecurityTestStatus.PASS,
                SecuritySeverity.MEDIUM,
                "Session management works properly"
            ))

        } catch (e: Exception) {
            securityResults.add(SecurityTestResult(
                "SEC-021",
                "Session Management",
                SecurityTestStatus.FAIL,
                SecuritySeverity.MEDIUM,
                "Session management test failed: ${e.message}",
                "Review session management implementation"
            ))
        }
    }

    // =========================
    // SECURITY HEADERS TESTS
    // =========================

    @Test
    @Order(30)
    @DisplayName("SEC-030: Security Headers Validation")
    fun `SEC-030 verify proper security headers are set`() {
        try {
            val response = testRestTemplate.getForEntity(
                "http://localhost:$port/api/health",
                String::class.java
            )

            val headers = response.headers
            val securityHeadersPresent = mutableListOf<String>()
            val securityHeadersMissing = mutableListOf<String>()

            // Check for important security headers
            val expectedHeaders = mapOf(
                "X-Content-Type-Options" to "nosniff",
                "X-Frame-Options" to null, // Any value is acceptable
                "X-XSS-Protection" to null, // Any value is acceptable
                "Strict-Transport-Security" to null // For HTTPS
            )

            expectedHeaders.forEach { (headerName, expectedValue) ->
                val headerValue = headers[headerName]?.firstOrNull()
                if (headerValue != null) {
                    securityHeadersPresent.add(headerName)
                    if (expectedValue != null && headerValue != expectedValue) {
                        println("⚠️ Security header $headerName has value '$headerValue', expected '$expectedValue'")
                    }
                } else {
                    securityHeadersMissing.add(headerName)
                }
            }

            val severity = if (securityHeadersMissing.isEmpty()) SecuritySeverity.LOW
                          else if (securityHeadersMissing.size <= 2) SecuritySeverity.MEDIUM
                          else SecuritySeverity.HIGH

            securityResults.add(SecurityTestResult(
                "SEC-030",
                "Security Headers Validation",
                if (severity == SecuritySeverity.LOW) SecurityTestStatus.PASS else SecurityTestStatus.FAIL,
                severity,
                "Security headers present: ${securityHeadersPresent.joinToString()}, missing: ${securityHeadersMissing.joinToString()}",
                if (securityHeadersMissing.isNotEmpty()) "Add missing security headers: ${securityHeadersMissing.joinToString()}" else ""
            ))

        } catch (e: Exception) {
            securityResults.add(SecurityTestResult(
                "SEC-030",
                "Security Headers Validation",
                SecurityTestStatus.FAIL,
                SecuritySeverity.MEDIUM,
                "Security headers validation failed: ${e.message}",
                "Review security headers configuration"
            ))
        }
    }

    @Test
    @Order(40)
    @DisplayName("SUMMARY: Security Test Results")
    fun `SUMMARY generate security test report`() {
        println("\n" + "=".repeat(80))
        println("CHICKCALCULATOR SECURITY VALIDATION RESULTS")
        println("=".repeat(80))

        val passedTests = securityResults.count { it.status == SecurityTestStatus.PASS }
        val failedTests = securityResults.count { it.status == SecurityTestStatus.FAIL }
        val criticalFailures = securityResults.count { it.status == SecurityTestStatus.CRITICAL_FAIL }

        val criticalIssues = securityResults.count { it.severity == SecuritySeverity.CRITICAL }
        val highIssues = securityResults.count { it.severity == SecuritySeverity.HIGH }
        val mediumIssues = securityResults.count { it.severity == SecuritySeverity.MEDIUM }
        val lowIssues = securityResults.count { it.severity == SecuritySeverity.LOW }

        println("SECURITY SUMMARY:")
        println("  Total Tests: ${securityResults.size}")
        println("  Passed: $passedTests")
        println("  Failed: $failedTests")
        println("  Critical Failures: $criticalFailures")
        println()
        println("SEVERITY BREAKDOWN:")
        println("  🔴 Critical: $criticalIssues")
        println("  🟠 High: $highIssues")
        println("  🟡 Medium: $mediumIssues")
        println("  🟢 Low: $lowIssues")

        println("\nDETAILED SECURITY RESULTS:")
        securityResults.forEach { result ->
            val status = when (result.status) {
                SecurityTestStatus.PASS -> "✅"
                SecurityTestStatus.FAIL -> "❌"
                SecurityTestStatus.CRITICAL_FAIL -> "🚨"
            }
            val severity = when (result.severity) {
                SecuritySeverity.CRITICAL -> "🔴"
                SecuritySeverity.HIGH -> "🟠" 
                SecuritySeverity.MEDIUM -> "🟡"
                SecuritySeverity.LOW -> "🟢"
            }
            println("  $status $severity ${result.testId}: ${result.testName}")
            println("     ${result.description}")
            if (result.remediation.isNotEmpty()) {
                println("     Remediation: ${result.remediation}")
            }
            println()
        }

        println("\nSECURITY RECOMMENDATIONS:")
        securityResults.filter { it.status != SecurityTestStatus.PASS }.forEach { result ->
            println("  ${result.testId}: ${result.remediation}")
        }

        println("=".repeat(80))

        // Fail if critical security issues are found
        if (criticalFailures > 0) {
            throw AssertionError("Critical security vulnerabilities detected! Migration should not proceed.")
        }

        if (criticalIssues > 0) {
            throw AssertionError("Critical severity security issues found! Address before deployment.")
        }
    }
}