package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.MarinationLog
import com.example.chickencalculator.service.MarinationLogService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/marination-log")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With", "X-Location-Id"]
)
class MarinationLogController(
    private val marinationLogService: MarinationLogService
) {
    
    private val logger = LoggerFactory.getLogger(MarinationLogController::class.java)
    
    @GetMapping
    fun getAllMarinationLogs(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): List<MarinationLog> {
        val locationId = marinationLogService.resolveLocationId(locationIdHeader)
        return marinationLogService.getAllMarinationLogsByLocation(locationId)
    }
    
    @GetMapping("/today")
    fun getTodaysMarinationLogs(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): List<MarinationLog> {
        val locationId = marinationLogService.resolveLocationId(locationIdHeader)
        return marinationLogService.getTodaysMarinationLogsByLocation(locationId)
    }
    
    @PostMapping
    fun addMarinationLog(
        @RequestBody log: MarinationLog,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): MarinationLog {
        val locationId = marinationLogService.resolveLocationId(locationIdHeader)
        return marinationLogService.addMarinationLog(log, locationId)
    }
    
    @DeleteMapping("/{id}")
    fun deleteMarinationLog(
        @PathVariable id: Long,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ) {
        val locationId = marinationLogService.resolveLocationId(locationIdHeader)
        marinationLogService.deleteMarinationLog(id, locationId)
    }
}