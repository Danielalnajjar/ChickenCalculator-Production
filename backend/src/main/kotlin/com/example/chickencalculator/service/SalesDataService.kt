package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.SalesTotals
import com.example.chickencalculator.repository.LocationRepository
import com.example.chickencalculator.repository.SalesDataRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Isolation
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@Service
class SalesDataService(
    private val salesDataRepository: SalesDataRepository,
    private val locationRepository: LocationRepository
) {
    
    private val logger = LoggerFactory.getLogger(SalesDataService::class.java)
    
    /**
     * Get all sales data for a specific location
     */
    @Transactional(readOnly = true)
    fun getAllSalesDataByLocation(locationId: Long): List<SalesData> {
        validateLocationExists(locationId)
        return salesDataRepository.findByLocationIdOrderByDateDesc(locationId)
    }
    
    /**
     * Get sales totals for a specific location
     */
    @Transactional(readOnly = true)
    fun getSalesTotalsByLocation(locationId: Long): SalesTotals {
        validateLocationExists(locationId)
        return salesDataRepository.getSalesTotalsByLocation(locationId)
    }
    
    /**
     * Add new sales data for a location
     */
    @Transactional(rollbackFor = [Exception::class])
    fun addSalesData(salesData: SalesData, locationId: Long): SalesData {
        val location = getLocationById(locationId)
        
        // Validate that sales data for this date doesn't already exist
        val existingSalesData = salesDataRepository.findByLocationAndDate(location, salesData.date)
        if (existingSalesData != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT, 
                "Sales data for date ${salesData.date} already exists for this location"
            )
        }
        
        // Create new sales data with specified location
        val salesDataWithLocation = salesData.copy(location = location)
        
        logger.info("Adding sales data for location: ${location.name} (ID: $locationId) on date: ${salesData.date}")
        return salesDataRepository.save(salesDataWithLocation)
    }
    
    /**
     * Delete specific sales data entry
     */
    @Transactional(rollbackFor = [Exception::class])
    fun deleteSalesData(salesDataId: Long, locationId: Long) {
        val salesData = salesDataRepository.findById(salesDataId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Sales data not found")
        }
        
        // Security check: ensure the sales data belongs to the requested location
        if (salesData.location.id != locationId) {
            logger.warn("Attempted to delete sales data from different location. Data location: ${salesData.location.id}, Requested location: $locationId")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete sales data from different location")
        }
        
        salesDataRepository.deleteById(salesDataId)
        logger.info("Deleted sales data ID: $salesDataId for location: $locationId")
    }
    
    /**
     * Delete all sales data for a location
     */
    @Transactional(rollbackFor = [Exception::class])
    fun deleteAllSalesDataByLocation(locationId: Long) {
        // Validate that the location exists
        validateLocationExists(locationId)
        
        val deletedCount = salesDataRepository.findByLocationIdOrderByDateDesc(locationId).size
        salesDataRepository.deleteByLocationId(locationId)
        logger.info("Deleted $deletedCount sales data entries for location: $locationId")
    }
    
    /**
     * Get sales data for a specific date and location
     */
    @Transactional(readOnly = true)
    fun getSalesDataByLocationAndDate(locationId: Long, date: LocalDate): SalesData? {
        val location = getLocationById(locationId)
        return salesDataRepository.findByLocationAndDate(location, date)
    }
    
    /**
     * Get sales data within a date range for a location
     */
    @Transactional(readOnly = true)
    fun getSalesDataByLocationAndDateRange(
        locationId: Long, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<SalesData> {
        val location = getLocationById(locationId)
        return salesDataRepository.findByLocationAndDateBetweenOrderByDateDesc(location, startDate, endDate)
    }
    
    /**
     * Get sales totals for a date range
     */
    @Transactional(readOnly = true)
    fun getSalesTotalsByLocationAndDateRange(
        locationId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): SalesTotals {
        val location = getLocationById(locationId)
        return salesDataRepository.getSalesTotalsByLocationAndDateRange(location, startDate, endDate)
    }
    
    /**
     * Update existing sales data
     */
    @Transactional(rollbackFor = [Exception::class])
    fun updateSalesData(salesDataId: Long, updatedSalesData: SalesData, locationId: Long): SalesData {
        val existingSalesData = salesDataRepository.findById(salesDataId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Sales data not found")
        }
        
        // Security check: ensure the sales data belongs to the requested location
        if (existingSalesData.location.id != locationId) {
            logger.warn("Attempted to update sales data from different location. Data location: ${existingSalesData.location.id}, Requested location: $locationId")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update sales data from different location")
        }
        
        // Create updated sales data while preserving the existing location and ID
        val updatedData = updatedSalesData.copy(
            id = existingSalesData.id,
            location = existingSalesData.location
        )
        
        logger.info("Updating sales data ID: $salesDataId for location: $locationId")
        return salesDataRepository.save(updatedData)
    }
    
    /**
     * Extract location ID from request header or fallback to default location
     * This ensures backward compatibility while enabling multi-tenant support
     */
    @Transactional(readOnly = true)
    fun resolveLocationId(locationIdHeader: String?): Long {
        return if (locationIdHeader != null) {
            try {
                val locationId = locationIdHeader.toLong()
                validateLocationExists(locationId)
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