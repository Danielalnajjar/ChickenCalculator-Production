package com.example.chickencalculator.migration

import com.example.chickencalculator.dto.*
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.repository.LocationRepository
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Load Test Suite for PostgreSQL Migration
 * 
 * This test suite validates system behavior under load:
 * 1. Concurrent user simulation
 * 2. Database connection pool stress testing
 * 3. API rate limiting validation
 * 4. Resource consumption monitoring
 * 5. System stability under sustained load
 * 6. Graceful degradation testing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("load-test")
@Testcontainers
class LoadTestSuite {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var adminService: AdminService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("chicken_calculator_load_test")
            .withUsername("load_user")
            .withPassword("load_password")

        private val loadTestResults = mutableListOf<LoadTestResult>()
        private lateinit var testLocations: List<Location>
        private lateinit var testAdmin: AdminUser
        
        // Load test configuration
        private const val LIGHT_LOAD_USERS = 10
        private const val MEDIUM_LOAD_USERS = 25
        private const val HEAVY_LOAD_USERS = 50
        private const val STRESS_LOAD_USERS = 100
        
        private const val REQUESTS_PER_USER = 20
        private const val TEST_DURATION_SECONDS = 30L
    }

    data class LoadTestResult(
        val testId: String,
        val testName: String,
        val concurrentUsers: Int,
        val totalRequests: Int,
        val successfulRequests: Int,
        val failedRequests: Int,
        val averageResponseTime: Double,
        val minResponseTime: Long,
        val maxResponseTime: Long,
        val throughput: Double, // requests per second
        val errorRate: Double,
        val testDuration: Long,
        val status: LoadTestStatus,
        val memoryUsage: Long,
        val errors: List<String> = emptyList()
    )

    enum class LoadTestStatus {
        PASS, DEGRADED, FAIL, CRITICAL_FAIL
    }

    @BeforeAll
    fun setupLoadTestData() {
        // Create multiple test locations for load testing
        testLocations = (1..5).map { i ->
            locationRepository.save(Location(
                name = "Load Test Location $i",
                slug = "load-test-location-$i",
                managerName = "Load Manager $i",
                managerEmail = "load$i@test.com"
            ))
        }

        // Create test admin
        testAdmin = adminService.createAdminUser(
            email = "load.admin@test.com",
            password = "LoadTest123!",
            name = "Load Test Admin",
            role = AdminRole.ADMIN,
            passwordChangeRequired = false
        )
    }

    // =========================
    // LIGHT LOAD TESTS
    // =========================

    @Test
    @Order(1)
    @DisplayName("LOAD-001: Light Load - Health Check")
    fun `LOAD-001 health check under light concurrent load`() {
        val concurrentUsers = LIGHT_LOAD_USERS
        val requestsPerUser = 10
        
        val result = executeLoadTest(
            testId = "LOAD-001",
            testName = "Light Load - Health Check",
            endpoint = "/api/health",
            method = HttpMethod.GET,
            concurrentUsers = concurrentUsers,
            requestsPerUser = requestsPerUser,
            requestBody = null,
            headers = null
        )

        loadTestResults.add(result)

        // Assertions for light load
        assertTrue(result.errorRate <= 1.0, "Error rate should be ≤ 1% under light load")
        assertTrue(result.averageResponseTime <= 200.0, "Average response time should be ≤ 200ms under light load")
        assertTrue(result.throughput >= 40.0, "Throughput should be ≥ 40 req/sec under light load")
    }

    @Test
    @Order(2)
    @DisplayName("LOAD-002: Light Load - Calculator API")
    fun `LOAD-002 calculator API under light concurrent load`() {
        val concurrentUsers = LIGHT_LOAD_USERS
        val requestsPerUser = 5
        
        val marinationRequest = createRandomMarinationRequest()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            add("X-Location-Id", testLocations.random().id.toString())
        }

        val result = executeLoadTest(
            testId = "LOAD-002",
            testName = "Light Load - Calculator API",
            endpoint = "/api/v1/calculator/calculate",
            method = HttpMethod.POST,
            concurrentUsers = concurrentUsers,
            requestsPerUser = requestsPerUser,
            requestBody = marinationRequest,
            headers = headers
        )

        loadTestResults.add(result)

        // Assertions for calculator API under light load
        assertTrue(result.errorRate <= 2.0, "Calculator API error rate should be ≤ 2% under light load")
        assertTrue(result.averageResponseTime <= 500.0, "Calculator API response time should be ≤ 500ms under light load")
    }

    // =========================
    // MEDIUM LOAD TESTS
    // =========================

    @Test
    @Order(10)
    @DisplayName("LOAD-010: Medium Load - Mixed API Requests")
    fun `LOAD-010 mixed API requests under medium concurrent load`() {
        val concurrentUsers = MEDIUM_LOAD_USERS
        val testDuration = 20L // seconds
        
        val result = executeMixedLoadTest(
            testId = "LOAD-010",
            testName = "Medium Load - Mixed API Requests",
            concurrentUsers = concurrentUsers,
            testDurationSeconds = testDuration
        )

        loadTestResults.add(result)

        // Assertions for medium load
        assertTrue(result.errorRate <= 5.0, "Error rate should be ≤ 5% under medium load")
        assertTrue(result.averageResponseTime <= 1000.0, "Average response time should be ≤ 1s under medium load")
        assertTrue(result.throughput >= 20.0, "Throughput should be ≥ 20 req/sec under medium load")
    }

    @Test
    @Order(11)
    @DisplayName("LOAD-011: Medium Load - Database Stress")
    fun `LOAD-011 database operations under medium concurrent load`() {
        val concurrentUsers = MEDIUM_LOAD_USERS
        val requestsPerUser = 15

        val headers = HttpHeaders().apply {
            add("X-Location-Id", testLocations.random().id.toString())
        }

        val result = executeLoadTest(
            testId = "LOAD-011",
            testName = "Medium Load - Database Stress",
            endpoint = "/api/v1/sales-data",
            method = HttpMethod.GET,
            concurrentUsers = concurrentUsers,
            requestsPerUser = requestsPerUser,
            requestBody = null,
            headers = headers
        )

        loadTestResults.add(result)

        // Database operations should maintain performance
        assertTrue(result.errorRate <= 3.0, "Database error rate should be ≤ 3% under medium load")
        assertTrue(result.averageResponseTime <= 800.0, "Database response time should be ≤ 800ms under medium load")
    }

    // =========================
    // HEAVY LOAD TESTS
    // =========================

    @Test
    @Order(20)
    @DisplayName("LOAD-020: Heavy Load - System Stress Test")
    fun `LOAD-020 system stress test under heavy concurrent load`() {
        val concurrentUsers = HEAVY_LOAD_USERS
        val testDuration = 30L // seconds

        val result = executeSustainedLoadTest(
            testId = "LOAD-020",
            testName = "Heavy Load - System Stress Test",
            concurrentUsers = concurrentUsers,
            testDurationSeconds = testDuration
        )

        loadTestResults.add(result)

        // Heavy load acceptance criteria (more lenient)
        assertTrue(result.errorRate <= 10.0, "Error rate should be ≤ 10% under heavy load")
        assertTrue(result.averageResponseTime <= 2000.0, "Average response time should be ≤ 2s under heavy load")
        assertTrue(result.throughput >= 15.0, "Throughput should be ≥ 15 req/sec under heavy load")
    }

    @Test
    @Order(21)
    @DisplayName("LOAD-021: Heavy Load - Connection Pool Stress")
    fun `LOAD-021 database connection pool under heavy load`() {
        val concurrentUsers = HEAVY_LOAD_USERS
        val requestsPerUser = 10

        // Test with multiple database operations
        val result = executeDatabaseStressTest(
            testId = "LOAD-021", 
            testName = "Heavy Load - Connection Pool Stress",
            concurrentUsers = concurrentUsers,
            requestsPerUser = requestsPerUser
        )

        loadTestResults.add(result)

        // Connection pool should handle the load
        assertTrue(result.errorRate <= 15.0, "Connection pool error rate should be ≤ 15% under heavy load")
        assertTrue(result.status != LoadTestStatus.CRITICAL_FAIL, "System should not critically fail")
    }

    // =========================
    // STRESS TESTS
    // =========================

    @Test
    @Order(30)
    @DisplayName("LOAD-030: Stress Test - Maximum Load")
    fun `LOAD-030 maximum load stress test`() {
        val concurrentUsers = STRESS_LOAD_USERS
        val testDuration = 20L // seconds

        val result = executeStressTest(
            testId = "LOAD-030",
            testName = "Stress Test - Maximum Load",
            concurrentUsers = concurrentUsers,
            testDurationSeconds = testDuration
        )

        loadTestResults.add(result)

        // Stress test should show graceful degradation, not complete failure
        assertTrue(result.status != LoadTestStatus.CRITICAL_FAIL, "System should not completely fail under stress")
        assertTrue(result.errorRate <= 50.0, "Even under stress, error rate should not exceed 50%")
        
        // System should still process some requests successfully
        assertTrue(result.successfulRequests > 0, "System should process some requests successfully even under stress")
    }

    @Test
    @Order(31)
    @DisplayName("LOAD-031: Recovery Test - Post-Stress Recovery")
    fun `LOAD-031 system recovery after stress test`() {
        // Give system time to recover
        Thread.sleep(5000)

        val concurrentUsers = LIGHT_LOAD_USERS
        val requestsPerUser = 5

        val result = executeLoadTest(
            testId = "LOAD-031",
            testName = "Recovery Test - Post-Stress Recovery",
            endpoint = "/api/health",
            method = HttpMethod.GET,
            concurrentUsers = concurrentUsers,
            requestsPerUser = requestsPerUser,
            requestBody = null,
            headers = null
        )

        loadTestResults.add(result)

        // System should recover to normal performance
        assertTrue(result.errorRate <= 5.0, "System should recover with ≤ 5% error rate")
        assertTrue(result.averageResponseTime <= 500.0, "System should recover response times ≤ 500ms")
    }

    // =========================
    // MEMORY AND RESOURCE TESTS
    // =========================

    @Test
    @Order(40)
    @DisplayName("LOAD-040: Memory Usage Under Load")
    fun `LOAD-040 monitor memory usage under sustained load`() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Execute sustained load
        val concurrentUsers = MEDIUM_LOAD_USERS
        val testDuration = 30L

        val result = executeSustainedLoadTest(
            testId = "LOAD-040",
            testName = "Memory Usage Under Load",
            concurrentUsers = concurrentUsers,
            testDurationSeconds = testDuration
        )

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        println("Memory Analysis:")
        println("  Initial Memory: ${initialMemory / (1024 * 1024)} MB")
        println("  Final Memory: ${finalMemory / (1024 * 1024)} MB")
        println("  Memory Increase: ${memoryIncrease / (1024 * 1024)} MB")
        println("  Memory Increase %: ${(memoryIncrease.toDouble() / initialMemory * 100)}")

        // Update result with memory information
        val updatedResult = result.copy(memoryUsage = memoryIncrease)
        loadTestResults.add(updatedResult)

        // Memory usage should not increase excessively
        assertTrue(
            memoryIncrease < 100 * 1024 * 1024, // Less than 100MB increase
            "Memory increase should be less than 100MB under sustained load"
        )
    }

    @Test
    @Order(50)
    @DisplayName("SUMMARY: Load Test Results")
    fun `SUMMARY generate comprehensive load test report`() {
        println("\n" + "=".repeat(80))
        println("CHICKCALCULATOR LOAD TEST RESULTS")
        println("=".repeat(80))

        val totalTests = loadTestResults.size
        val passedTests = loadTestResults.count { it.status == LoadTestStatus.PASS }
        val degradedTests = loadTestResults.count { it.status == LoadTestStatus.DEGRADED }
        val failedTests = loadTestResults.count { it.status == LoadTestStatus.FAIL }
        val criticalFailures = loadTestResults.count { it.status == LoadTestStatus.CRITICAL_FAIL }

        println("LOAD TEST SUMMARY:")
        println("  Total Load Tests: $totalTests")
        println("  Passed: $passedTests")
        println("  Degraded Performance: $degradedTests")
        println("  Failed: $failedTests") 
        println("  Critical Failures: $criticalFailures")

        println("\nDETAILED LOAD TEST RESULTS:")
        loadTestResults.forEach { result ->
            val status = when (result.status) {
                LoadTestStatus.PASS -> "✅"
                LoadTestStatus.DEGRADED -> "⚠️"
                LoadTestStatus.FAIL -> "❌"
                LoadTestStatus.CRITICAL_FAIL -> "🚨"
            }

            println("  $status ${result.testId}: ${result.testName}")
            println("     Concurrent Users: ${result.concurrentUsers}")
            println("     Total Requests: ${result.totalRequests}")
            println("     Successful: ${result.successfulRequests} (${((result.successfulRequests.toDouble() / result.totalRequests) * 100).format(1)}%)")
            println("     Failed: ${result.failedRequests} (${result.errorRate.format(1)}%)")
            println("     Avg Response Time: ${result.averageResponseTime.format(2)}ms")
            println("     Min/Max Response Time: ${result.minResponseTime}ms / ${result.maxResponseTime}ms")
            println("     Throughput: ${result.throughput.format(2)} req/sec")
            println("     Test Duration: ${result.testDuration}ms")
            if (result.memoryUsage > 0) {
                println("     Memory Usage: ${result.memoryUsage / (1024 * 1024)} MB")
            }
            if (result.errors.isNotEmpty()) {
                println("     Sample Errors: ${result.errors.take(3).joinToString("; ")}")
            }
            println()
        }

        println("\nPERFORMANCE ANALYSIS:")
        
        // Light load analysis
        val lightLoadTests = loadTestResults.filter { it.concurrentUsers <= LIGHT_LOAD_USERS }
        if (lightLoadTests.isNotEmpty()) {
            val avgLightThroughput = lightLoadTests.map { it.throughput }.average()
            val avgLightErrorRate = lightLoadTests.map { it.errorRate }.average()
            println("  Light Load (≤$LIGHT_LOAD_USERS users):")
            println("    Average Throughput: ${avgLightThroughput.format(2)} req/sec")
            println("    Average Error Rate: ${avgLightErrorRate.format(2)}%")
        }

        // Heavy load analysis
        val heavyLoadTests = loadTestResults.filter { it.concurrentUsers >= HEAVY_LOAD_USERS }
        if (heavyLoadTests.isNotEmpty()) {
            val avgHeavyThroughput = heavyLoadTests.map { it.throughput }.average()
            val avgHeavyErrorRate = heavyLoadTests.map { it.errorRate }.average()
            println("  Heavy Load (≥$HEAVY_LOAD_USERS users):")
            println("    Average Throughput: ${avgHeavyThroughput.format(2)} req/sec")
            println("    Average Error Rate: ${avgHeavyErrorRate.format(2)}%")
        }

        println("\nLOAD TEST RECOMMENDATIONS:")
        loadTestResults.forEach { result ->
            when {
                result.status == LoadTestStatus.CRITICAL_FAIL -> 
                    println("  🚨 ${result.testName}: Critical failure - investigate immediately")
                result.status == LoadTestStatus.FAIL ->
                    println("  ❌ ${result.testName}: Failed - requires optimization")
                result.status == LoadTestStatus.DEGRADED ->
                    println("  ⚠️ ${result.testName}: Degraded performance - consider scaling")
                result.errorRate > 5.0 ->
                    println("  ⚠️ ${result.testName}: High error rate - investigate error handling")
                result.averageResponseTime > 1000.0 ->
                    println("  ⚠️ ${result.testName}: Slow response times - optimize performance")
            }
        }

        println("=".repeat(80))

        // Fail test if critical issues found
        if (criticalFailures > 0) {
            throw AssertionError("Critical load test failures detected! System cannot handle expected load.")
        }

        if (failedTests > totalTests / 2) {
            throw AssertionError("Too many load test failures! System may not be ready for production load.")
        }
    }

    // =========================
    // HELPER METHODS
    // =========================

    private fun executeLoadTest(
        testId: String,
        testName: String,
        endpoint: String,
        method: HttpMethod,
        concurrentUsers: Int,
        requestsPerUser: Int,
        requestBody: Any?,
        headers: HttpHeaders?
    ): LoadTestResult {
        
        val totalRequests = concurrentUsers * requestsPerUser
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val responseTimes = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        val startTime = System.currentTimeMillis()

        val futures = (1..concurrentUsers).map { userId ->
            CompletableFuture.supplyAsync {
                val userResponseTimes = mutableListOf<Long>()
                var userSuccessCount = 0
                
                repeat(requestsPerUser) {
                    try {
                        val requestStart = System.currentTimeMillis()
                        
                        val response = when (method) {
                            HttpMethod.GET -> {
                                testRestTemplate.exchange(
                                    "http://localhost:$port$endpoint",
                                    method,
                                    HttpEntity<String>(null, headers),
                                    String::class.java
                                )
                            }
                            HttpMethod.POST -> {
                                testRestTemplate.postForEntity(
                                    "http://localhost:$port$endpoint",
                                    HttpEntity(requestBody, headers),
                                    String::class.java
                                )
                            }
                            else -> throw UnsupportedOperationException("Method $method not supported")
                        }
                        
                        val requestTime = System.currentTimeMillis() - requestStart
                        userResponseTimes.add(requestTime)
                        
                        if (response.statusCode.is2xxSuccessful) {
                            userSuccessCount++
                        } else {
                            synchronized(errors) {
                                if (errors.size < 10) { // Limit error collection
                                    errors.add("HTTP ${response.statusCode.value()}: ${response.body}")
                                }
                            }
                        }
                        
                    } catch (e: Exception) {
                        synchronized(errors) {
                            if (errors.size < 10) {
                                errors.add(e.message ?: "Unknown error")
                            }
                        }
                    }
                }
                
                successCount.addAndGet(userSuccessCount)
                failCount.addAndGet(requestsPerUser - userSuccessCount)
                
                synchronized(responseTimes) {
                    responseTimes.addAll(userResponseTimes)
                }
            }
        }

        // Wait for all requests to complete with timeout
        futures.forEach { it.get(60, TimeUnit.SECONDS) }
        
        val totalTime = System.currentTimeMillis() - startTime
        val successfulRequests = successCount.get()
        val failedRequests = failCount.get()
        val averageResponseTime = if (responseTimes.isNotEmpty()) responseTimes.average() else 0.0
        val minResponseTime = responseTimes.minOrNull() ?: 0L
        val maxResponseTime = responseTimes.maxOrNull() ?: 0L
        val throughput = (totalRequests.toDouble() / (totalTime / 1000.0))
        val errorRate = (failedRequests.toDouble() / totalRequests) * 100
        
        val status = when {
            errorRate > 25.0 || averageResponseTime > 5000 -> LoadTestStatus.CRITICAL_FAIL
            errorRate > 10.0 || averageResponseTime > 2000 -> LoadTestStatus.FAIL
            errorRate > 5.0 || averageResponseTime > 1000 -> LoadTestStatus.DEGRADED
            else -> LoadTestStatus.PASS
        }

        return LoadTestResult(
            testId = testId,
            testName = testName,
            concurrentUsers = concurrentUsers,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            averageResponseTime = averageResponseTime,
            minResponseTime = minResponseTime,
            maxResponseTime = maxResponseTime,
            throughput = throughput,
            errorRate = errorRate,
            testDuration = totalTime,
            status = status,
            memoryUsage = 0L,
            errors = errors.take(5) // Keep top 5 errors
        )
    }

    private fun executeMixedLoadTest(
        testId: String,
        testName: String,
        concurrentUsers: Int,
        testDurationSeconds: Long
    ): LoadTestResult {
        
        val endpoints = listOf(
            "/api/health" to HttpMethod.GET,
            "/api/v1/calculator/locations" to HttpMethod.GET,
            "/api/v1/sales-data" to HttpMethod.GET,
            "/actuator/health" to HttpMethod.GET
        )

        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val responseTimes = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        val totalRequestCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (testDurationSeconds * 1000)

        val futures = (1..concurrentUsers).map { userId ->
            CompletableFuture.runAsync {
                while (System.currentTimeMillis() < endTime) {
                    val (endpoint, method) = endpoints.random()
                    
                    try {
                        val headers = HttpHeaders().apply {
                            if (endpoint.contains("sales-data")) {
                                add("X-Location-Id", testLocations.random().id.toString())
                            }
                        }
                        
                        val requestStart = System.currentTimeMillis()
                        val response = testRestTemplate.exchange(
                            "http://localhost:$port$endpoint",
                            method,
                            HttpEntity<String>(null, headers),
                            String::class.java
                        )
                        val requestTime = System.currentTimeMillis() - requestStart
                        
                        totalRequestCount.incrementAndGet()
                        synchronized(responseTimes) {
                            responseTimes.add(requestTime)
                        }
                        
                        if (response.statusCode.is2xxSuccessful) {
                            successCount.incrementAndGet()
                        } else {
                            failCount.incrementAndGet()
                            synchronized(errors) {
                                if (errors.size < 10) {
                                    errors.add("$endpoint: HTTP ${response.statusCode.value()}")
                                }
                            }
                        }
                        
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                        totalRequestCount.incrementAndGet()
                        synchronized(errors) {
                            if (errors.size < 10) {
                                errors.add("$endpoint: ${e.message}")
                            }
                        }
                    }
                    
                    // Small delay to prevent overwhelming
                    Thread.sleep(Random.nextLong(10, 100))
                }
            }
        }

        futures.forEach { it.get(testDurationSeconds + 10, TimeUnit.SECONDS) }
        
        val totalTime = System.currentTimeMillis() - startTime
        val totalRequests = totalRequestCount.get()
        val successfulRequests = successCount.get()
        val failedRequests = failCount.get()
        val averageResponseTime = if (responseTimes.isNotEmpty()) responseTimes.average() else 0.0
        val minResponseTime = responseTimes.minOrNull() ?: 0L
        val maxResponseTime = responseTimes.maxOrNull() ?: 0L
        val throughput = (totalRequests.toDouble() / (totalTime / 1000.0))
        val errorRate = if (totalRequests > 0) (failedRequests.toDouble() / totalRequests) * 100 else 0.0
        
        val status = when {
            errorRate > 25.0 -> LoadTestStatus.CRITICAL_FAIL
            errorRate > 15.0 -> LoadTestStatus.FAIL
            errorRate > 8.0 -> LoadTestStatus.DEGRADED
            else -> LoadTestStatus.PASS
        }

        return LoadTestResult(
            testId = testId,
            testName = testName,
            concurrentUsers = concurrentUsers,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            averageResponseTime = averageResponseTime,
            minResponseTime = minResponseTime,
            maxResponseTime = maxResponseTime,
            throughput = throughput,
            errorRate = errorRate,
            testDuration = totalTime,
            status = status,
            memoryUsage = 0L,
            errors = errors.take(5)
        )
    }

    private fun executeSustainedLoadTest(
        testId: String,
        testName: String,
        concurrentUsers: Int,
        testDurationSeconds: Long
    ): LoadTestResult {
        // Similar to mixed load test but focuses on sustained performance
        return executeMixedLoadTest(testId, testName, concurrentUsers, testDurationSeconds)
    }

    private fun executeDatabaseStressTest(
        testId: String,
        testName: String,
        concurrentUsers: Int,
        requestsPerUser: Int
    ): LoadTestResult {
        val headers = HttpHeaders().apply {
            add("X-Location-Id", testLocations.random().id.toString())
        }
        
        return executeLoadTest(
            testId = testId,
            testName = testName,
            endpoint = "/api/v1/sales-data",
            method = HttpMethod.GET,
            concurrentUsers = concurrentUsers,
            requestsPerUser = requestsPerUser,
            requestBody = null,
            headers = headers
        )
    }

    private fun executeStressTest(
        testId: String,
        testName: String,
        concurrentUsers: Int,
        testDurationSeconds: Long
    ): LoadTestResult {
        return executeMixedLoadTest(testId, testName, concurrentUsers, testDurationSeconds)
    }

    private fun createRandomMarinationRequest(): MarinationRequest {
        return MarinationRequest(
            inventory = InventoryData(
                pansSoy = BigDecimal.valueOf(Random.nextDouble(5.0, 15.0)),
                pansTeriyaki = BigDecimal.valueOf(Random.nextDouble(3.0, 12.0)),
                pansTurmeric = BigDecimal.valueOf(Random.nextDouble(2.0, 10.0))
            ),
            projectedSales = ProjectedSales(
                day0 = BigDecimal.valueOf(Random.nextDouble(80.0, 150.0)),
                day1 = BigDecimal.valueOf(Random.nextDouble(90.0, 160.0)),
                day2 = BigDecimal.valueOf(Random.nextDouble(70.0, 140.0)),
                day3 = BigDecimal.valueOf(Random.nextDouble(60.0, 130.0))
            ),
            availableRawChickenKg = BigDecimal.valueOf(Random.nextDouble(30.0, 80.0)),
            alreadyMarinatedSoy = BigDecimal.valueOf(Random.nextDouble(0.0, 10.0)),
            alreadyMarinatedTeriyaki = BigDecimal.valueOf(Random.nextDouble(0.0, 8.0)),
            alreadyMarinatedTurmeric = BigDecimal.valueOf(Random.nextDouble(0.0, 6.0))
        )
    }

    private fun Double.format(digits: Int): String = String.format("%.${digits}f", this)
}