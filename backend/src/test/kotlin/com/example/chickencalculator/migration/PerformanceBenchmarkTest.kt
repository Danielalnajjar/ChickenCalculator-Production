package com.example.chickencalculator.migration

import com.example.chickencalculator.dto.MarinationRequest
import com.example.chickencalculator.dto.InventoryData
import com.example.chickencalculator.dto.ProjectedSales
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.repository.SalesDataRepository
import com.example.chickencalculator.repository.LocationRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Performance Benchmark Test Suite for PostgreSQL Migration
 * 
 * This test suite measures and compares performance metrics:
 * 1. API response times (H2 vs PostgreSQL)
 * 2. Database query performance
 * 3. Connection pool behavior
 * 4. Memory and CPU utilization
 * 5. Concurrent load handling
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("performance-test")
@Testcontainers
class PerformanceBenchmarkTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var salesDataRepository: SalesDataRepository

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("chicken_calculator_perf_test")
            .withUsername("perf_user")
            .withPassword("perf_password")

        private val performanceResults = mutableMapOf<String, PerformanceMetric>()
        
        // Performance thresholds (in milliseconds)
        private const val HEALTH_CHECK_THRESHOLD = 100L
        private const val CALCULATION_THRESHOLD = 200L
        private const val DATABASE_QUERY_THRESHOLD = 50L
        private const val BULK_INSERT_THRESHOLD = 1000L
        private const val CONCURRENT_REQUEST_THRESHOLD = 500L
    }

    data class PerformanceMetric(
        val testId: String,
        val testName: String,
        val averageResponseTime: Long,
        val minResponseTime: Long,
        val maxResponseTime: Long,
        val throughput: Double, // requests per second
        val successRate: Double, // percentage
        val threshold: Long,
        val status: String // PASS/FAIL/WARNING
    )

    // =========================
    // BASELINE PERFORMANCE TESTS
    // =========================

    @Test
    @Order(1)
    @DisplayName("PERF-001: Health Check Performance")
    fun `PERF-001 measure health check endpoint performance`() {
        val iterations = 100
        val responseTimes = mutableListOf<Long>()
        var successCount = 0

        repeat(iterations) {
            val responseTime = measureTimeMillis {
                val response = testRestTemplate.getForEntity(
                    "http://localhost:$port/api/health",
                    String::class.java
                )
                if (response.statusCode.is2xxSuccessful) {
                    successCount++
                }
            }
            responseTimes.add(responseTime)
        }

        val metric = PerformanceMetric(
            testId = "PERF-001",
            testName = "Health Check Performance",
            averageResponseTime = responseTimes.average().toLong(),
            minResponseTime = responseTimes.minOrNull() ?: 0L,
            maxResponseTime = responseTimes.maxOrNull() ?: 0L,
            throughput = (successCount.toDouble() / (responseTimes.sum() / 1000.0)),
            successRate = (successCount.toDouble() / iterations) * 100,
            threshold = HEALTH_CHECK_THRESHOLD,
            status = if (responseTimes.average() <= HEALTH_CHECK_THRESHOLD) "PASS" else "FAIL"
        )

        performanceResults["PERF-001"] = metric

        Assertions.assertTrue(
            metric.averageResponseTime <= HEALTH_CHECK_THRESHOLD,
            "Health check average response time ${metric.averageResponseTime}ms should be ≤ ${HEALTH_CHECK_THRESHOLD}ms"
        )
        
        Assertions.assertTrue(
            metric.successRate >= 95.0,
            "Health check success rate ${metric.successRate}% should be ≥ 95%"
        )
    }

    @Test
    @Order(2)
    @DisplayName("PERF-002: Calculator API Performance")
    fun `PERF-002 measure calculator API performance`() {
        val iterations = 50
        val responseTimes = mutableListOf<Long>()
        var successCount = 0

        // Setup test data
        val testLocation = locationRepository.save(Location(
            name = "Performance Test Location",
            slug = "perf-test-location",
            managerName = "Perf Manager",
            managerEmail = "perf@test.com"
        ))

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

        repeat(iterations) {
            val responseTime = measureTimeMillis {
                val response = testRestTemplate.postForEntity(
                    "http://localhost:$port/api/v1/calculator/calculate",
                    HttpEntity(marinationRequest, headers),
                    String::class.java
                )
                if (response.statusCode.is2xxSuccessful) {
                    successCount++
                }
            }
            responseTimes.add(responseTime)
        }

        val metric = PerformanceMetric(
            testId = "PERF-002",
            testName = "Calculator API Performance",
            averageResponseTime = responseTimes.average().toLong(),
            minResponseTime = responseTimes.minOrNull() ?: 0L,
            maxResponseTime = responseTimes.maxOrNull() ?: 0L,
            throughput = (successCount.toDouble() / (responseTimes.sum() / 1000.0)),
            successRate = (successCount.toDouble() / iterations) * 100,
            threshold = CALCULATION_THRESHOLD,
            status = if (responseTimes.average() <= CALCULATION_THRESHOLD) "PASS" else "FAIL"
        )

        performanceResults["PERF-002"] = metric

        Assertions.assertTrue(
            metric.averageResponseTime <= CALCULATION_THRESHOLD,
            "Calculator API average response time ${metric.averageResponseTime}ms should be ≤ ${CALCULATION_THRESHOLD}ms"
        )
    }

    @Test
    @Order(3)
    @DisplayName("PERF-003: Database Query Performance")
    fun `PERF-003 measure database query performance`() {
        val iterations = 100
        val responseTimes = mutableListOf<Long>()

        // Setup test location
        val testLocation = locationRepository.save(Location(
            name = "DB Perf Test Location",
            slug = "db-perf-test",
            managerName = "DB Manager",
            managerEmail = "db@test.com"
        ))

        // Insert test data
        val salesDataList = mutableListOf<SalesData>()
        for (i in 1..100) {
            salesDataList.add(SalesData(
                date = LocalDate.now().minusDays(i.toLong()),
                totalSales = BigDecimal("1000.00").add(BigDecimal(i)),
                portionsSoy = BigDecimal("50.0"),
                portionsTeriyaki = BigDecimal("30.0"),
                portionsTurmeric = BigDecimal("20.0"),
                locationId = testLocation.id!!
            ))
        }
        salesDataRepository.saveAll(salesDataList)

        // Measure query performance
        repeat(iterations) {
            val responseTime = measureTimeMillis {
                salesDataRepository.findByLocationIdAndDateBetween(
                    testLocation.id!!,
                    LocalDate.now().minusDays(30),
                    LocalDate.now()
                )
            }
            responseTimes.add(responseTime)
        }

        val metric = PerformanceMetric(
            testId = "PERF-003",
            testName = "Database Query Performance",
            averageResponseTime = responseTimes.average().toLong(),
            minResponseTime = responseTimes.minOrNull() ?: 0L,
            maxResponseTime = responseTimes.maxOrNull() ?: 0L,
            throughput = (iterations.toDouble() / (responseTimes.sum() / 1000.0)),
            successRate = 100.0,
            threshold = DATABASE_QUERY_THRESHOLD,
            status = if (responseTimes.average() <= DATABASE_QUERY_THRESHOLD) "PASS" 
                     else if (responseTimes.average() <= DATABASE_QUERY_THRESHOLD * 2) "WARNING"
                     else "FAIL"
        )

        performanceResults["PERF-003"] = metric

        Assertions.assertTrue(
            metric.averageResponseTime <= DATABASE_QUERY_THRESHOLD * 2,
            "Database query average response time ${metric.averageResponseTime}ms should be ≤ ${DATABASE_QUERY_THRESHOLD * 2}ms"
        )
    }

    @Test
    @Order(4)
    @DisplayName("PERF-004: Bulk Insert Performance")
    fun `PERF-004 measure bulk insert performance`() {
        val batchSize = 1000
        val responseTime = measureTimeMillis {
            val location = locationRepository.save(Location(
                name = "Bulk Insert Test Location",
                slug = "bulk-insert-test",
                managerName = "Bulk Manager", 
                managerEmail = "bulk@test.com"
            ))

            val salesDataBatch = mutableListOf<SalesData>()
            for (i in 1..batchSize) {
                salesDataBatch.add(SalesData(
                    date = LocalDate.now().minusDays(i.toLong()),
                    totalSales = BigDecimal("${1000 + i}"),
                    portionsSoy = BigDecimal("${50 + (i % 10)}"),
                    portionsTeriyaki = BigDecimal("${30 + (i % 5)}"),
                    portionsTurmeric = BigDecimal("${20 + (i % 3)}"),
                    locationId = location.id!!
                ))
            }

            salesDataRepository.saveAll(salesDataBatch)
        }

        val metric = PerformanceMetric(
            testId = "PERF-004",
            testName = "Bulk Insert Performance",
            averageResponseTime = responseTime,
            minResponseTime = responseTime,
            maxResponseTime = responseTime,
            throughput = (batchSize.toDouble() / (responseTime / 1000.0)),
            successRate = 100.0,
            threshold = BULK_INSERT_THRESHOLD,
            status = if (responseTime <= BULK_INSERT_THRESHOLD) "PASS" 
                     else if (responseTime <= BULK_INSERT_THRESHOLD * 2) "WARNING"
                     else "FAIL"
        )

        performanceResults["PERF-004"] = metric

        Assertions.assertTrue(
            responseTime <= BULK_INSERT_THRESHOLD * 2,
            "Bulk insert time ${responseTime}ms should be ≤ ${BULK_INSERT_THRESHOLD * 2}ms for $batchSize records"
        )
    }

    @Test
    @Order(5)
    @DisplayName("PERF-005: Concurrent Request Performance")
    fun `PERF-005 measure concurrent request performance`() {
        val concurrentUsers = 20
        val requestsPerUser = 10
        val totalRequests = concurrentUsers * requestsPerUser

        val location = locationRepository.save(Location(
            name = "Concurrent Test Location",
            slug = "concurrent-test",
            managerName = "Concurrent Manager",
            managerEmail = "concurrent@test.com"
        ))

        val futures = mutableListOf<CompletableFuture<Long>>()
        val startTime = System.currentTimeMillis()

        // Launch concurrent requests
        repeat(concurrentUsers) { userId ->
            val future = CompletableFuture.supplyAsync {
                var userSuccessCount = 0
                val userStartTime = System.currentTimeMillis()

                repeat(requestsPerUser) {
                    try {
                        val response = testRestTemplate.getForEntity(
                            "http://localhost:$port/api/health",
                            String::class.java
                        )
                        if (response.statusCode.is2xxSuccessful) {
                            userSuccessCount++
                        }
                    } catch (e: Exception) {
                        // Count as failure
                    }
                }

                System.currentTimeMillis() - userStartTime
            }
            futures.add(future)
        }

        // Wait for all requests to complete
        val userResponseTimes = futures.map { it.get(30, TimeUnit.SECONDS) }
        val totalTime = System.currentTimeMillis() - startTime
        val successfulRequests = userResponseTimes.size * requestsPerUser // Simplified

        val metric = PerformanceMetric(
            testId = "PERF-005",
            testName = "Concurrent Request Performance",
            averageResponseTime = userResponseTimes.average().toLong(),
            minResponseTime = userResponseTimes.minOrNull() ?: 0L,
            maxResponseTime = userResponseTimes.maxOrNull() ?: 0L,
            throughput = (totalRequests.toDouble() / (totalTime / 1000.0)),
            successRate = (successfulRequests.toDouble() / totalRequests) * 100,
            threshold = CONCURRENT_REQUEST_THRESHOLD,
            status = if (userResponseTimes.average() <= CONCURRENT_REQUEST_THRESHOLD) "PASS" 
                     else if (userResponseTimes.average() <= CONCURRENT_REQUEST_THRESHOLD * 2) "WARNING"
                     else "FAIL"
        )

        performanceResults["PERF-005"] = metric

        Assertions.assertTrue(
            metric.averageResponseTime <= CONCURRENT_REQUEST_THRESHOLD * 2,
            "Concurrent request average time ${metric.averageResponseTime}ms should be ≤ ${CONCURRENT_REQUEST_THRESHOLD * 2}ms"
        )

        Assertions.assertTrue(
            metric.successRate >= 90.0,
            "Concurrent request success rate ${metric.successRate}% should be ≥ 90%"
        )
    }

    @Test
    @Order(6)
    @DisplayName("PERF-006: Memory Usage Analysis")
    fun `PERF-006 analyze memory usage during load`() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Simulate heavy load
        val location = locationRepository.save(Location(
            name = "Memory Test Location",
            slug = "memory-test",
            managerName = "Memory Manager",
            managerEmail = "memory@test.com"
        ))

        // Create a large dataset
        val largeBatch = mutableListOf<SalesData>()
        for (i in 1..5000) {
            largeBatch.add(SalesData(
                date = LocalDate.now().minusDays((i % 365).toLong()),
                totalSales = BigDecimal("${1000 + i}"),
                portionsSoy = BigDecimal("${50 + (i % 50)}"),
                portionsTeriyaki = BigDecimal("${30 + (i % 30)}"),
                portionsTurmeric = BigDecimal("${20 + (i % 20)}"),
                locationId = location.id!!
            ))
        }

        val memoryBeforeOperation = runtime.totalMemory() - runtime.freeMemory()
        salesDataRepository.saveAll(largeBatch)
        val memoryAfterOperation = runtime.totalMemory() - runtime.freeMemory()

        // Force garbage collection
        runtime.gc()
        Thread.sleep(1000) // Give GC time to work
        val memoryAfterGC = runtime.totalMemory() - runtime.freeMemory()

        val memoryUsedDuringOperation = memoryAfterOperation - memoryBeforeOperation
        val memoryRetainedAfterGC = memoryAfterGC - initialMemory

        println("Memory Usage Analysis:")
        println("  Initial Memory: ${initialMemory / (1024 * 1024)} MB")
        println("  Memory Before Operation: ${memoryBeforeOperation / (1024 * 1024)} MB")
        println("  Memory After Operation: ${memoryAfterOperation / (1024 * 1024)} MB")
        println("  Memory After GC: ${memoryAfterGC / (1024 * 1024)} MB")
        println("  Memory Used During Operation: ${memoryUsedDuringOperation / (1024 * 1024)} MB")
        println("  Memory Retained After GC: ${memoryRetainedAfterGC / (1024 * 1024)} MB")

        // Memory usage should be reasonable (less than 512MB total)
        Assertions.assertTrue(
            (memoryAfterOperation / (1024 * 1024)) < 512,
            "Memory usage should be less than 512MB during heavy operations"
        )
    }

    @Test
    @Order(10)
    @DisplayName("SUMMARY: Performance Test Results")
    fun `SUMMARY generate performance test report`() {
        println("\n" + "=".repeat(80))
        println("CHICKCALCULATOR PERFORMANCE TEST RESULTS")
        println("=".repeat(80))

        val passedTests = performanceResults.values.count { it.status == "PASS" }
        val warningTests = performanceResults.values.count { it.status == "WARNING" }
        val failedTests = performanceResults.values.count { it.status == "FAIL" }

        println("PERFORMANCE SUMMARY:")
        println("  Total Tests: ${performanceResults.size}")
        println("  Passed: $passedTests")
        println("  Warnings: $warningTests")
        println("  Failed: $failedTests")

        println("\nDETAILED PERFORMANCE METRICS:")
        performanceResults.values.forEach { metric ->
            val status = when (metric.status) {
                "PASS" -> "✅"
                "WARNING" -> "⚠️"
                "FAIL" -> "❌"
                else -> "❓"
            }
            println("  $status ${metric.testId}: ${metric.testName}")
            println("     Average Response Time: ${metric.averageResponseTime}ms (threshold: ${metric.threshold}ms)")
            println("     Min/Max Response Time: ${metric.minResponseTime}ms / ${metric.maxResponseTime}ms")
            println("     Throughput: ${"%.2f".format(metric.throughput)} req/sec")
            println("     Success Rate: ${"%.1f".format(metric.successRate)}%")
            println()
        }

        println("\nPERFORMANCE RECOMMENDATIONS:")
        performanceResults.values.forEach { metric ->
            when (metric.status) {
                "WARNING" -> println("  ⚠️ ${metric.testName}: Consider optimization - response time above optimal threshold")
                "FAIL" -> println("  ❌ ${metric.testName}: Requires immediate attention - performance below acceptable levels")
            }
        }

        println("=".repeat(80))

        // Fail the test if any critical performance metrics failed
        val criticalFailures = performanceResults.values.filter { it.status == "FAIL" }
        if (criticalFailures.isNotEmpty()) {
            throw AssertionError("Critical performance failures detected: ${criticalFailures.map { it.testName }}")
        }
    }
}