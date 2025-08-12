package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.MarinationLog
import com.example.chickencalculator.repository.LocationRepository
import com.example.chickencalculator.repository.MarinationLogRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Isolation
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class MarinationLogService(
    private val marinationLogRepository: MarinationLogRepository,
    private val locationRepository: LocationRepository
) {
    
    private val logger = LoggerFactory.getLogger(MarinationLogService::class.java)
    
    /**
     * Get all marination logs for a specific location
     */
    @Transactional(readOnly = true)
    fun getAllMarinationLogsByLocation(locationId: Long): List<MarinationLog> {
        val location = getLocationById(locationId)
        return marinationLogRepository.findByLocationOrderByTimestampDesc(location)
    }
    
    /**
     * Get today's marination logs for a specific location
     */
    @Transactional(readOnly = true)
    fun getTodaysMarinationLogsByLocation(locationId: Long): List<MarinationLog> {
        val location = getLocationById(locationId)
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.plusDays(1).atStartOfDay()
        
        return marinationLogRepository.findByLocationAndTimestampBetween(
            location,
            startOfDay,
            endOfDay
        )
    }
    
    /**
     * Get marination logs for a specific date range
     */
    @Transactional(readOnly = true)
    fun getMarinationLogsByLocationAndDateRange(
        locationId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<MarinationLog> {
        val location = getLocationById(locationId)
        return marinationLogRepository.findByLocationAndTimestampBetween(location, startDate, endDate)
    }
    
    /**
     * Add new marination log for a location
     */
    @Transactional(rollbackFor = [Exception::class])
    fun addMarinationLog(marinationLog: MarinationLog, locationId: Long): MarinationLog {
        val location = getLocationById(locationId)
        
        // Validate business rules
        validateMarinationLogData(marinationLog)
        
        // Create new marination log with specified location and current timestamp
        val logWithLocation = marinationLog.copy(
            location = location,
            timestamp = LocalDateTime.now()
        )
        
        logger.info("Adding marination log for location: ${location.name} (ID: $locationId) at ${logWithLocation.timestamp}")
        return marinationLogRepository.save(logWithLocation)
    }
    
    /**
     * Delete specific marination log entry
     */
    @Transactional(rollbackFor = [Exception::class])
    fun deleteMarinationLog(marinationLogId: Long, locationId: Long) {
        val log = marinationLogRepository.findById(marinationLogId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Marination log not found")
        }
        
        // Security check: ensure the marination log belongs to the requested location
        if (log.location.id != locationId) {
            logger.warn("Attempted to delete marination log from different location. Log location: ${log.location.id}, Requested location: $locationId")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete marination log from different location")
        }
        
        marinationLogRepository.deleteById(marinationLogId)
        logger.info("Deleted marination log ID: $marinationLogId for location: $locationId")
    }
    
    /**
     * Delete all marination logs for a location
     */
    @Transactional(rollbackFor = [Exception::class])
    fun deleteAllMarinationLogsByLocation(locationId: Long) {
        val location = getLocationById(locationId)
        
        val deletedCount = marinationLogRepository.findByLocationOrderByTimestampDesc(location).size
        marinationLogRepository.deleteByLocation(location)
        logger.info("Deleted $deletedCount marination log entries for location: $locationId")
    }
    
    /**
     * Update existing marination log
     */
    @Transactional(rollbackFor = [Exception::class])
    fun updateMarinationLog(marinationLogId: Long, updatedLog: MarinationLog, locationId: Long): MarinationLog {
        val existingLog = marinationLogRepository.findById(marinationLogId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Marination log not found")
        }
        
        // Security check: ensure the marination log belongs to the requested location
        if (existingLog.location.id != locationId) {
            logger.warn("Attempted to update marination log from different location. Log location: ${existingLog.location.id}, Requested location: $locationId")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update marination log from different location")
        }
        
        // Validate business rules
        validateMarinationLogData(updatedLog)
        
        // Create updated marination log while preserving the existing location, ID, and timestamp
        val updatedData = updatedLog.copy(
            id = existingLog.id,
            location = existingLog.location,
            timestamp = existingLog.timestamp
        )
        
        logger.info("Updating marination log ID: $marinationLogId for location: $locationId")
        return marinationLogRepository.save(updatedData)
    }
    
    /**
     * Get the latest marination log for a location
     */
    @Transactional(readOnly = true)
    fun getLatestMarinationLogByLocation(locationId: Long): MarinationLog? {
        val location = getLocationById(locationId)
        return marinationLogRepository.findByLocationOrderByTimestampDesc(location).firstOrNull()
    }
    
    /**
     * Get end-of-day marination logs for a location
     */
    @Transactional(readOnly = true)
    fun getEndOfDayLogsByLocation(locationId: Long): List<MarinationLog> {
        val location = getLocationById(locationId)
        return marinationLogRepository.findByLocationAndIsEndOfDayTrueOrderByTimestampDesc(location)
    }
    
    /**
     * Extract location ID from request header - no fallback to default
     * Location context must be explicitly provided for multi-tenant isolation
     */
    @Transactional(readOnly = true)
    fun resolveLocationId(locationIdHeader: String?): Long {
        if (locationIdHeader == null) {
            logger.error("No location context provided in request")
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Location context is required. Please access through a location-specific URL."
            )
        }
        
        return try {
            val locationId = locationIdHeader.toLong()
            validateLocationExists(locationId)
            locationId
        } catch (e: NumberFormatException) {
            logger.error("Invalid location ID format: $locationIdHeader")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid location ID format")
        }
    }
    
    /**
     * Validate marination log business rules
     */
    private fun validateMarinationLogData(marinationLog: MarinationLog) {
        // Validate that suggested quantities are not negative
        if (marinationLog.soySuggested < 0.toBigDecimal() ||
            marinationLog.teriyakiSuggested < 0.toBigDecimal() ||
            marinationLog.turmericSuggested < 0.toBigDecimal()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Suggested quantities cannot be negative")
        }
        
        // Validate that pan quantities are not negative
        if (marinationLog.soyPans < 0.toBigDecimal() ||
            marinationLog.teriyakiPans < 0.toBigDecimal() ||
            marinationLog.turmericPans < 0.toBigDecimal()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Pan quantities cannot be negative")
        }
        
        // Business rule: pan quantities should not exceed suggested quantities by more than 50%
        val tolerance = 1.5.toBigDecimal()
        if (marinationLog.soyPans > marinationLog.soySuggested.multiply(tolerance) ||
            marinationLog.teriyakiPans > marinationLog.teriyakiSuggested.multiply(tolerance) ||
            marinationLog.turmericPans > marinationLog.turmericSuggested.multiply(tolerance)) {
            logger.warn("Pan quantities significantly exceed suggested quantities for marination log")
        }
    }
    
    /**
     * Validate that a location exists
     */
    private fun validateLocationExists(locationId: Long) {
        if (!locationRepository.existsById(locationId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Location with ID $locationId not found")
        }
    }
    
    /**
     * Get location by ID or throw exception
     */
    private fun getLocationById(locationId: Long): Location {
        return locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location with ID $locationId not found")
        }
    }
}