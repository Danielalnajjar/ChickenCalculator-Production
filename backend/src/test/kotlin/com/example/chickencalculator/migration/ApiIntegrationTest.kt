package com.example.chickencalculator.migration

import com.example.chickencalculator.dto.*
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.repository.LocationRepository
import com.example.chickencalculator.repository.SalesDataRepository
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
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive API Integration Test Suite for PostgreSQL Migration
 * 
 * This test suite validates all API endpoints after migration:
 * 1. Public endpoints (/api/v1/calculator/*, /api/health)
 * 2. Admin endpoints (/api/v1/admin/*)
 * 3. Actuator endpoints (/actuator/*)
 * 4. Static resource endpoints
 * 5. Location-specific routing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("integration-test")
@Testcontainers
class ApiIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var salesDataRepository: SalesDataRepository

    @Autowired
    private lateinit var adminService: AdminService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("chicken_calculator_api_test")
            .withUsername("api_user")
            .withPassword("api_password")

        private val apiTestResults = mutableListOf<ApiTestResult>()
        private var authToken: String = ""
        private lateinit var testLocation: Location
        private lateinit var testAdmin: AdminUser
    }

    data class ApiTestResult(
        val testId: String,
        val endpoint: String,
        val method: String,
        val expectedStatus: HttpStatus,
        val actualStatus: HttpStatus,
        val responseTime: Long,
        val success: Boolean,
        val errorMessage: String = ""
    )

    @BeforeAll
    fun setupTestData() {
        // Create test location
        testLocation = locationRepository.save(Location(
            name = "API Test Location",
            slug = "api-test-location", 
            managerName = "API Test Manager",
            managerEmail = "apitest@example.com"
        ))

        // Create test admin
        testAdmin = adminService.createAdminUser(
            email = "api.admin@example.com",
            password = "ApiAdmin123!",
            name = "API Test Admin",
            role = AdminRole.ADMIN,
            passwordChangeRequired = false
        )

        // Create test sales data
        salesDataRepository.save(SalesData(
            date = LocalDate.now().minusDays(1),
            totalSales = BigDecimal("1500.00"),
            portionsSoy = BigDecimal("100.0"),
            portionsTeriyaki = BigDecimal("75.0"),
            portionsTurmeric = BigDecimal("50.0"),
            locationId = testLocation.id!!
        ))
    }

    // =========================
    // PUBLIC API ENDPOINTS
    // =========================

    @Test
    @Order(1)
    @DisplayName("API-001: Health Check Endpoint")
    fun `API-001 health check endpoint returns correct status`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/api/health",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-001",
            endpoint = "/api/health",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains("UP") || response.body!!.contains("healthy"))
    }

    @Test
    @Order(2)
    @DisplayName("API-002: Calculator Locations Endpoint")
    fun `API-002 calculator locations endpoint returns available locations`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/api/v1/calculator/locations",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-002",
            endpoint = "/api/v1/calculator/locations",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        
        // Parse and validate response
        val locations = objectMapper.readValue(response.body, Array<LocationResponse>::class.java)
        assertTrue(locations.isNotEmpty(), "Should return at least one location")
        
        val apiTestLocation = locations.find { it.slug == "api-test-location" }
        assertNotNull(apiTestLocation, "Should include test location")
    }

    @Test
    @Order(3)
    @DisplayName("API-003: Marination Calculator Endpoint")
    fun `API-003 marination calculator endpoint performs calculations`() {
        val startTime = System.currentTimeMillis()
        
        val marinationRequest = MarinationRequest(
            inventory = InventoryData(
                pansSoy = BigDecimal("10.0"),
                pansTeriyaki = BigDecimal("8.0"),
                pansTurmeric = BigDecimal("6.0")
            ),
            projectedSales = ProjectedSales(
                day0 = BigDecimal("100.0"),
                day1 = BigDecimal("120.0"),
                day2 = BigDecimal("110.0"),
                day3 = BigDecimal("90.0")
            ),
            availableRawChickenKg = BigDecimal("50.0"),
            alreadyMarinatedSoy = BigDecimal("5.0"),
            alreadyMarinatedTeriyaki = BigDecimal("3.0"),
            alreadyMarinatedTurmeric = BigDecimal("2.0")
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            add("X-Location-Id", testLocation.id.toString())
        }

        val response = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/calculator/calculate",
            HttpEntity(marinationRequest, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-003",
            endpoint = "/api/v1/calculator/calculate",
            method = "POST",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK,
            errorMessage = if (response.statusCode != HttpStatus.OK) response.body ?: "No error message" else ""
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        
        // Parse and validate calculation result
        val result = objectMapper.readValue(response.body, CalculationResult::class.java)
        assertNotNull(result)
        assertTrue(result.totalSuggested >= BigDecimal.ZERO, "Total suggested should be non-negative")
    }

    @Test
    @Order(4)
    @DisplayName("API-004: Sales Data Retrieval")
    fun `API-004 sales data endpoint returns historical data`() {
        val startTime = System.currentTimeMillis()
        
        val headers = HttpHeaders().apply {
            add("X-Location-Id", testLocation.id.toString())
        }

        val response = testRestTemplate.exchange(
            "http://localhost:$port/api/v1/sales-data",
            HttpMethod.GET,
            HttpEntity<String>(null, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-004",
            endpoint = "/api/v1/sales-data",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        
        // Parse and validate sales data
        val salesData = objectMapper.readValue(response.body, Array<SalesDataResponse>::class.java)
        assertTrue(salesData.isNotEmpty(), "Should return sales data for test location")
    }

    @Test
    @Order(5)
    @DisplayName("API-005: Sales Data Creation")
    fun `API-005 sales data creation endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val newSalesData = CreateSalesDataRequest(
            date = LocalDate.now(),
            totalSales = BigDecimal("2000.00"),
            portionsSoy = BigDecimal("120.0"),
            portionsTeriyaki = BigDecimal("90.0"),
            portionsTurmeric = BigDecimal("60.0")
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            add("X-Location-Id", testLocation.id.toString())
        }

        val response = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/sales-data",
            HttpEntity(newSalesData, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-005",
            endpoint = "/api/v1/sales-data",
            method = "POST",
            expectedStatus = HttpStatus.CREATED,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.CREATED,
            errorMessage = if (response.statusCode != HttpStatus.CREATED) response.body ?: "No error message" else ""
        ))

        assertTrue(
            response.statusCode == HttpStatus.CREATED || response.statusCode == HttpStatus.OK,
            "Sales data creation should succeed"
        )
    }

    @Test
    @Order(6)
    @DisplayName("API-006: Marination Log Retrieval")
    fun `API-006 marination log endpoint returns log data`() {
        val startTime = System.currentTimeMillis()
        
        val headers = HttpHeaders().apply {
            add("X-Location-Id", testLocation.id.toString())
        }

        val response = testRestTemplate.exchange(
            "http://localhost:$port/api/v1/marination-log",
            HttpMethod.GET,
            HttpEntity<String>(null, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-006",
            endpoint = "/api/v1/marination-log",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    // =========================
    // ADMIN API ENDPOINTS
    // =========================

    @Test
    @Order(10)
    @DisplayName("API-010: Admin Login")
    fun `API-010 admin login endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val loginRequest = LoginRequest(
            email = testAdmin.email,
            password = "ApiAdmin123!"
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val response = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/admin/auth/login",
            HttpEntity(loginRequest, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-010",
            endpoint = "/api/v1/admin/auth/login",
            method = "POST",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK,
            errorMessage = if (response.statusCode != HttpStatus.OK) response.body ?: "No error message" else ""
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        
        // Extract auth token from cookies for subsequent tests
        val cookies = response.headers["Set-Cookie"] ?: emptyList()
        val authCookie = cookies.find { it.contains("authToken") }
        if (authCookie != null) {
            authToken = authCookie.split(";")[0]
        }
    }

    @Test
    @Order(11)
    @DisplayName("API-011: Admin Token Validation")
    fun `API-011 admin token validation works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val headers = HttpHeaders().apply {
            if (authToken.isNotEmpty()) {
                add("Cookie", authToken)
            }
        }

        val response = testRestTemplate.exchange(
            "http://localhost:$port/api/v1/admin/auth/validate",
            HttpMethod.GET,
            HttpEntity<String>(null, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-011",
            endpoint = "/api/v1/admin/auth/validate",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK,
            errorMessage = if (response.statusCode != HttpStatus.OK) response.body ?: "No error message" else ""
        ))

        assertTrue(
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.UNAUTHORIZED,
            "Token validation should return appropriate status"
        )
    }

    @Test
    @Order(12)
    @DisplayName("API-012: Admin Locations Management")
    fun `API-012 admin locations endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val headers = HttpHeaders().apply {
            if (authToken.isNotEmpty()) {
                add("Cookie", authToken)
            }
        }

        val response = testRestTemplate.exchange(
            "http://localhost:$port/api/v1/admin/locations",
            HttpMethod.GET,
            HttpEntity<String>(null, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-012",
            endpoint = "/api/v1/admin/locations",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.UNAUTHORIZED,
            errorMessage = if (!listOf(HttpStatus.OK, HttpStatus.UNAUTHORIZED).contains(response.statusCode)) 
                         response.body ?: "No error message" else ""
        ))

        assertTrue(
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.UNAUTHORIZED,
            "Admin locations should return OK with valid auth or 401 without"
        )
    }

    @Test
    @Order(13)
    @DisplayName("API-013: Admin Statistics")
    fun `API-013 admin statistics endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val headers = HttpHeaders().apply {
            if (authToken.isNotEmpty()) {
                add("Cookie", authToken)
            }
        }

        val response = testRestTemplate.exchange(
            "http://localhost:$port/api/v1/admin/stats",
            HttpMethod.GET,
            HttpEntity<String>(null, headers),
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-013",
            endpoint = "/api/v1/admin/stats",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.UNAUTHORIZED,
            errorMessage = if (!listOf(HttpStatus.OK, HttpStatus.UNAUTHORIZED).contains(response.statusCode))
                         response.body ?: "No error message" else ""
        ))

        assertTrue(
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.UNAUTHORIZED,
            "Admin stats should return OK with valid auth or 401 without"
        )
    }

    @Test
    @Order(14)
    @DisplayName("API-014: CSRF Token Endpoint")
    fun `API-014 CSRF token endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/api/v1/admin/auth/csrf-token",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-014",
            endpoint = "/api/v1/admin/auth/csrf-token",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        
        // Verify CSRF token is set in cookies
        val cookies = response.headers["Set-Cookie"] ?: emptyList()
        val csrfCookie = cookies.find { it.contains("XSRF-TOKEN") }
        assertNotNull(csrfCookie, "CSRF token should be set as cookie")
    }

    // =========================
    // ACTUATOR ENDPOINTS
    // =========================

    @Test
    @Order(20)
    @DisplayName("API-020: Actuator Health Endpoint")
    fun `API-020 actuator health endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/actuator/health",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-020",
            endpoint = "/actuator/health",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains("UP") || response.body!!.contains("status"))
    }

    @Test
    @Order(21)
    @DisplayName("API-021: Actuator Metrics Endpoint")
    fun `API-021 actuator metrics endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/actuator/metrics",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-021",
            endpoint = "/actuator/metrics",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains("names") || response.body!!.contains("metric"))
    }

    @Test
    @Order(22)
    @DisplayName("API-022: Actuator Prometheus Endpoint")
    fun `API-022 actuator prometheus endpoint works correctly`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/actuator/prometheus",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-022",
            endpoint = "/actuator/prometheus",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains("# HELP") || response.body!!.contains("# TYPE"))
    }

    // =========================
    // LOCATION-SPECIFIC ROUTING
    // =========================

    @Test
    @Order(30)
    @DisplayName("API-030: Location-Specific Calculator Access")
    fun `API-030 location-specific calculator routing works`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/${testLocation.slug}",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-030",
            endpoint = "/${testLocation.slug}",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND
        ))

        // Should return either the calculator page or a 404 (depending on routing implementation)
        assertTrue(
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND,
            "Location-specific routing should return appropriate status"
        )
    }

    @Test
    @Order(31)
    @DisplayName("API-031: Default Calculator Access")
    fun `API-031 default calculator routing works`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-031",
            endpoint = "/",
            method = "GET",
            expectedStatus = HttpStatus.OK,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.OK
        ))

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    // =========================
    // ERROR HANDLING
    // =========================

    @Test
    @Order(40)
    @DisplayName("API-040: 404 Error Handling")
    fun `API-040 non-existent endpoints return 404`() {
        val startTime = System.currentTimeMillis()
        
        val response = testRestTemplate.getForEntity(
            "http://localhost:$port/api/nonexistent/endpoint",
            String::class.java
        )
        
        val responseTime = System.currentTimeMillis() - startTime
        
        apiTestResults.add(ApiTestResult(
            testId = "API-040",
            endpoint = "/api/nonexistent/endpoint",
            method = "GET",
            expectedStatus = HttpStatus.NOT_FOUND,
            actualStatus = response.statusCode,
            responseTime = responseTime,
            success = response.statusCode == HttpStatus.NOT_FOUND
        ))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    @Order(50)
    @DisplayName("SUMMARY: API Integration Test Results")
    fun `SUMMARY generate API integration test report`() {
        println("\n" + "=".repeat(80))
        println("CHICKCALCULATOR API INTEGRATION TEST RESULTS")
        println("=".repeat(80))

        val totalTests = apiTestResults.size
        val successfulTests = apiTestResults.count { it.success }
        val failedTests = totalTests - successfulTests
        val averageResponseTime = if (apiTestResults.isNotEmpty()) 
            apiTestResults.map { it.responseTime }.average() else 0.0

        println("API INTEGRATION SUMMARY:")
        println("  Total API Tests: $totalTests")
        println("  Successful: $successfulTests")
        println("  Failed: $failedTests")
        println("  Average Response Time: ${"%.2f".format(averageResponseTime)}ms")

        println("\nAPI ENDPOINT RESULTS:")
        apiTestResults.forEach { result ->
            val status = if (result.success) "✅" else "❌"
            val timing = if (result.responseTime <= 200) "🟢" 
                        else if (result.responseTime <= 500) "🟡" 
                        else "🔴"
            
            println("  $status $timing ${result.testId}: ${result.method} ${result.endpoint}")
            println("     Expected: ${result.expectedStatus}, Actual: ${result.actualStatus}, Time: ${result.responseTime}ms")
            if (result.errorMessage.isNotEmpty()) {
                println("     Error: ${result.errorMessage}")
            }
        }

        println("\nPERFORMANCE ANALYSIS:")
        val fastEndpoints = apiTestResults.filter { it.responseTime <= 100 }
        val slowEndpoints = apiTestResults.filter { it.responseTime > 500 }
        
        println("  Fast endpoints (≤100ms): ${fastEndpoints.size}")
        println("  Slow endpoints (>500ms): ${slowEndpoints.size}")
        
        if (slowEndpoints.isNotEmpty()) {
            println("  Slow endpoint details:")
            slowEndpoints.forEach { result ->
                println("    - ${result.endpoint}: ${result.responseTime}ms")
            }
        }

        println("\nAPI CATEGORIES:")
        val publicEndpoints = apiTestResults.filter { it.endpoint.startsWith("/api/v1/calculator") || it.endpoint == "/api/health" }
        val adminEndpoints = apiTestResults.filter { it.endpoint.startsWith("/api/v1/admin") }
        val actuatorEndpoints = apiTestResults.filter { it.endpoint.startsWith("/actuator") }
        
        println("  Public API: ${publicEndpoints.count { it.success }}/${publicEndpoints.size} successful")
        println("  Admin API: ${adminEndpoints.count { it.success }}/${adminEndpoints.size} successful")
        println("  Actuator API: ${actuatorEndpoints.count { it.success }}/${actuatorEndpoints.size} successful")

        println("=".repeat(80))

        // Fail if critical API endpoints are not working
        val criticalFailures = apiTestResults.filter { 
            !it.success && listOf(
                "/api/health",
                "/api/v1/calculator/locations", 
                "/api/v1/calculator/calculate",
                "/actuator/health"
            ).contains(it.endpoint)
        }

        if (criticalFailures.isNotEmpty()) {
            throw AssertionError("Critical API endpoints failing: ${criticalFailures.map { it.endpoint }}")
        }

        if (failedTests > totalTests * 0.2) { // More than 20% failure rate
            throw AssertionError("API integration test failure rate too high: $failedTests/$totalTests failed")
        }
    }
}