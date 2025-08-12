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
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With", "X-Location-Id"]
)
class MarinationLogController(
    private val marinationLogRepository: MarinationLogRepository,
    private val locationRepository: LocationRepository
) {
    
    private val logger = LoggerFactory.getLogger(MarinationLogController::class.java)
    
    @GetMapping
    fun getAllMarinationLogs(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): List<MarinationLog> {
        val locationId = getLocationId(locationIdHeader)
        val location = locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        }
        return marinationLogRepository.findByLocationOrderByTimestampDesc(location)
    }
    
    @GetMapping("/today")
    fun getTodaysMarinationLogs(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): List<MarinationLog> {
        val locationId = getLocationId(locationIdHeader)
        val location = locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        }
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.plusDays(1).atStartOfDay()
        return marinationLogRepository.findByLocationAndTimestampBetween(
            location,
            startOfDay,
            endOfDay
        )
    }
    
    @PostMapping
    fun addMarinationLog(
        @RequestBody log: MarinationLog,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): MarinationLog {
        val locationId = getLocationId(locationIdHeader)
        val location = locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        }
        
        // Create new marination log with specified location
        val logWithLocation = log.copy(location = location)
        
        logger.info("Adding marination log for location: ${location.name} (ID: $locationId)")
        return marinationLogRepository.save(logWithLocation)
    }
    
    @DeleteMapping("/{id}")
    fun deleteMarinationLog(
        @PathVariable id: Long,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ) {
        val locationId = getLocationId(locationIdHeader)
        val log = marinationLogRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Marination log not found")
        }
        
        // Security check: ensure the marination log belongs to the requested location
        if (log.location?.id != locationId) {
            logger.warn("Attempted to delete marination log from different location. Log location: ${log.location?.id}, Requested location: $locationId")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete marination log from different location")
        }
        
        marinationLogRepository.deleteById(id)
        logger.info("Deleted marination log ID: $id for location: $locationId")
    }
    
    /**
     * Extract location ID from request header or fallback to default location
     * This ensures backward compatibility while enabling multi-tenant support
     */
    private fun getLocationId(locationIdHeader: String?): Long {
        return if (locationIdHeader != null) {
            try {
                val locationId = locationIdHeader.toLong()
                // Validate that the location exists
                locationRepository.findById(locationId).orElseThrow {
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Location with ID $locationId not found")
                }
                locationId
            } catch (e: NumberFormatException) {
                logger.error("Invalid location ID format: $locationIdHeader")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid location ID format")
            }
        } else {
            // Fallback to default location for backward compatibility
            val defaultLocation = locationRepository.findByIsDefaultTrue()
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No default location found")
            defaultLocation.id
        }
    }
}