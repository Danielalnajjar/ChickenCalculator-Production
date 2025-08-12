package com.example.chickencalculator.controller

import com.example.chickencalculator.config.ApiVersionConfig
import com.example.chickencalculator.entity.MarinationLog
import com.example.chickencalculator.service.MarinationLogService
import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${ApiVersionConfig.API_VERSION}/marination-log")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With", "X-Location-Id"]
)
class MarinationLogController(
    private val marinationLogService: MarinationLogService,
    private val metricsService: MetricsService
) {
    
    private val logger = LoggerFactory.getLogger(MarinationLogController::class.java)
    
    @GetMapping
    @Timed(value = "chicken.calculator.marination_log.get_all.time", description = "Time taken to get all marination logs")
    fun getAllMarinationLogs(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ): List<MarinationLog> {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val locationId = marinationLogService.resolveLocationId(locationIdHeader)
            val result = marinationLogService.getAllMarinationLogsByLocation(locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordMarinationOperation(locationSlug, result.size)
            metricsService.recordDatabaseOperation("marination_log_get_all", processingTime)
            
            result
        } catch (e: Exception) {
            metricsService.recordError("marination_log_get_all", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @GetMapping("/today")
    @Timed(value = "chicken.calculator.marination_log.get_today.time", description = "Time taken to get today's marination logs")
    fun getTodaysMarinationLogs(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ): List<MarinationLog> {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val locationId = marinationLogService.resolveLocationId(locationIdHeader)
            val result = marinationLogService.getTodaysMarinationLogsByLocation(locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordMarinationOperation(locationSlug, result.size)
            metricsService.recordDatabaseOperation("marination_log_get_today", processingTime)
            
            result
        } catch (e: Exception) {
            metricsService.recordError("marination_log_get_today", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @PostMapping
    @Timed(value = "chicken.calculator.marination_log.add.time", description = "Time taken to add marination log")
    fun addMarinationLog(
        @RequestBody log: MarinationLog,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ): MarinationLog {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val locationId = marinationLogService.resolveLocationId(locationIdHeader)
            val result = marinationLogService.addMarinationLog(log, locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordMarinationOperation(locationSlug, 1)
            metricsService.recordDatabaseOperation("marination_log_add", processingTime)
            
            result
        } catch (e: Exception) {
            metricsService.recordError("marination_log_add", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @DeleteMapping("/{id}")
    @Timed(value = "chicken.calculator.marination_log.delete.time", description = "Time taken to delete marination log")
    fun deleteMarinationLog(
        @PathVariable id: Long,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ) {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        try {
            val locationId = marinationLogService.resolveLocationId(locationIdHeader)
            marinationLogService.deleteMarinationLog(id, locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordMarinationOperation(locationSlug, -1)
            metricsService.recordDatabaseOperation("marination_log_delete", processingTime)
        } catch (e: Exception) {
            metricsService.recordError("marination_log_delete", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
}