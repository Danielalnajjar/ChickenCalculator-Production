package com.example.chickencalculator.controller

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
    private val dataSource: DataSource
) : HealthIndicator {
    
    private val logger = LoggerFactory.getLogger(HealthController::class.java)
    private val jdbcTemplate = JdbcTemplate(dataSource)
    
    @GetMapping
    fun getHealth(): HealthStatus {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val services = mutableMapOf<String, ServiceHealth>()
        
        // Check database health
        services["database"] = checkDatabaseHealth()
        
        // Check application health
        services["application"] = ServiceHealth(
            status = "UP",
            details = mapOf(
                "version" to "1.0.0",
                "environment" to (System.getenv("SPRING_PROFILES_ACTIVE") ?: "default")
            )
        )
        
        val overallStatus = if (services.values.all { it.status == "UP" }) "UP" else "DOWN"
        
        return HealthStatus(
            status = overallStatus,
            timestamp = timestamp,
            services = services
        )
    }
    
    @GetMapping("/live")
    fun liveness(): Map<String, String> {
        return mapOf("status" to "UP")
    }
    
    @GetMapping("/ready")
    fun readiness(): Map<String, Any> {
        val dbHealth = checkDatabaseHealth()
        val isReady = dbHealth.status == "UP"
        
        return mapOf(
            "status" to if (isReady) "UP" else "DOWN",
            "database" to dbHealth.status
        )
    }
    
    private fun checkDatabaseHealth(): ServiceHealth {
        return try {
            val result = jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            if (result == 1) {
                ServiceHealth(
                    status = "UP",
                    details = mapOf(
                        "type" to dataSource.connection.metaData.databaseProductName,
                        "version" to dataSource.connection.metaData.databaseProductVersion
                    )
                )
            } else {
                ServiceHealth(status = "DOWN")
            }
        } catch (e: Exception) {
            logger.error("Database health check failed", e)
            ServiceHealth(
                status = "DOWN",
                details = mapOf("error" to (e.message ?: "Unknown error"))
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