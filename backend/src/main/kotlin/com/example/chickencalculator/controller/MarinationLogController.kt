package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.MarinationLog
import com.example.chickencalculator.repository.LocationRepository
import com.example.chickencalculator.repository.MarinationLogRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/marination-log")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"]
)
class MarinationLogController(
    private val marinationLogRepository: MarinationLogRepository,
    private val locationRepository: LocationRepository
) {
    
    private val logger = LoggerFactory.getLogger(MarinationLogController::class.java)
    
    @GetMapping
    fun getAllMarinationLogs(): List<MarinationLog> {
        // Return marination logs for default location only (public access)
        val defaultLocation = locationRepository.findByIsDefaultTrue()
        return if (defaultLocation != null) {
            marinationLogRepository.findByLocationOrderByTimestampDesc(defaultLocation)
        } else {
            emptyList()
        }
    }
    
    @GetMapping("/today")
    fun getTodaysMarinationLogs(): List<MarinationLog> {
        val defaultLocation = locationRepository.findByIsDefaultTrue()
        return if (defaultLocation != null) {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay()
            val endOfDay = today.plusDays(1).atStartOfDay()
            marinationLogRepository.findByLocationAndTimestampBetween(
                defaultLocation,
                startOfDay,
                endOfDay
            )
        } else {
            emptyList()
        }
    }
    
    @PostMapping
    fun addMarinationLog(@RequestBody log: MarinationLog): MarinationLog {
        // Auto-assign default location if not provided
        val defaultLocation = locationRepository.findByIsDefaultTrue()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default location not found")
        
        // Create new marination log with default location
        val logWithLocation = log.copy(location = defaultLocation)
        
        logger.info("Adding marination log for default location: ${defaultLocation.name}")
        return marinationLogRepository.save(logWithLocation)
    }
    
    @DeleteMapping("/{id}")
    fun deleteMarinationLog(@PathVariable id: Long) {
        // Only allow deletion of marination logs from default location
        val defaultLocation = locationRepository.findByIsDefaultTrue()
        if (defaultLocation != null) {
            val log = marinationLogRepository.findById(id).orElse(null)
            if (log?.location?.id == defaultLocation.id) {
                marinationLogRepository.deleteById(id)
            }
        }
    }
}