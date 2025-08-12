package com.example.chickencalculator.controller

import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

data class HealthStatus(
    val status: String,
    val timestamp: String,
    val services: Map<String, ServiceHealth>
)

data class ServiceHealth(
    val status: String,
    val details: Map<String, Any>? = null
)

@RestController
@RequestMapping("/api/health")
class HealthController(
    private val dataSource: DataSource,
    private val metricsService: MetricsService
) : HealthIndicator {
    
    private val logger = LoggerFactory.getLogger(HealthController::class.java)
    private val jdbcTemplate = JdbcTemplate(dataSource)
    
    @GetMapping
    @Timed(value = "chicken.calculator.health.check.time", description = "Time taken for health check")
    fun getHealth(): HealthStatus {
        val startTime = System.currentTimeMillis()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val services = mutableMapOf<String, ServiceHealth>()
        
        try {
            // Check database health
            services["database"] = checkDatabaseHealth()
            
            // Check application health
            services["application"] = ServiceHealth(
                status = "UP",
                details = mapOf(
                    "version" to "1.0.0",
                    "environment" to (System.getenv("SPRING_PROFILES_ACTIVE") ?: "default"),
                    "metrics_summary" to metricsService.getMetricsSummary()
                )
            )
            
            val overallStatus = if (services.values.all { it.status == "UP" }) "UP" else "DOWN"
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordHealthCheck("overall", overallStatus == "UP", processingTime)
            
            return HealthStatus(
                status = overallStatus,
                timestamp = timestamp,
                services = services
            )
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordHealthCheck("overall", false, processingTime)
            metricsService.recordError("health_check", e.javaClass.simpleName)
            throw e
        }
    }
    
    @GetMapping("/live")
    @Timed(value = "chicken.calculator.health.liveness.time", description = "Time taken for liveness check")
    fun liveness(): Map<String, String> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = mapOf("status" to "UP")
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordHealthCheck("liveness", true, processingTime)
            result
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordHealthCheck("liveness", false, processingTime)
            throw e
        }
    }
    
    @GetMapping("/ready")
    @Timed(value = "chicken.calculator.health.readiness.time", description = "Time taken for readiness check")
    fun readiness(): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val dbHealth = checkDatabaseHealth()
            val isReady = dbHealth.status == "UP"
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordHealthCheck("readiness", isReady, processingTime)
            
            mapOf(
                "status" to if (isReady) "UP" else "DOWN",
                "database" to dbHealth.status
            )
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordHealthCheck("readiness", false, processingTime)
            throw e
        }
    }
    
    private fun checkDatabaseHealth(): ServiceHealth {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            val processingTime = System.currentTimeMillis() - startTime
            
            if (result == 1) {
                metricsService.recordHealthCheck("database", true, processingTime)
                ServiceHealth(
                    status = "UP",
                    details = mapOf(
                        "type" to dataSource.connection.metaData.databaseProductName,
                        "version" to dataSource.connection.metaData.databaseProductVersion,
                        "response_time_ms" to processingTime
                    )
                )
            } else {
                metricsService.recordHealthCheck("database", false, processingTime)
                ServiceHealth(status = "DOWN")
            }
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            logger.error("Database health check failed", e)
            metricsService.recordHealthCheck("database", false, processingTime)
            metricsService.recordError("database_health_check", e.javaClass.simpleName)
            ServiceHealth(
                status = "DOWN",
                details = mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "response_time_ms" to processingTime
                )
            )
        }
    }
    
    // Spring Boot Actuator HealthIndicator implementation
    override fun health(): Health {
        val healthStatus = getHealth()
        return if (healthStatus.status == "UP") {
            Health.up()
                .withDetail("timestamp", healthStatus.timestamp)
                .withDetail("services", healthStatus.services)
                .build()
        } else {
            Health.down()
                .withDetail("timestamp", healthStatus.timestamp)
                .withDetail("services", healthStatus.services)
                .build()
        }
    }
}