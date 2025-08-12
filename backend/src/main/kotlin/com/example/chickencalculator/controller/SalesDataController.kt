package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.SalesTotals
import com.example.chickencalculator.repository.LocationRepository
import com.example.chickencalculator.repository.SalesDataRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@RestController
@RequestMapping("/api/sales-data")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With", "X-Location-Id"]
)
class SalesDataController(
    private val salesDataRepository: SalesDataRepository,
    private val locationRepository: LocationRepository
) {
    
    private val logger = LoggerFactory.getLogger(SalesDataController::class.java)
    
    @GetMapping
    fun getAllSalesData(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): List<SalesData> {
        val locationId = getLocationId(locationIdHeader)
        return salesDataRepository.findByLocationIdOrderByDateDesc(locationId)
    }
    
    @GetMapping("/totals")
    fun getSalesTotals(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): SalesTotals {
        val locationId = getLocationId(locationIdHeader)
        return salesDataRepository.getSalesTotalsByLocation(locationId)
    }
    
    @PostMapping
    fun addSalesData(
        @RequestBody salesData: SalesData,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): SalesData {
        val locationId = getLocationId(locationIdHeader)
        val location = locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        }
        
        // Create new sales data with specified location
        val salesDataWithLocation = salesData.copy(location = location)
        
        logger.info("Adding sales data for location: ${location.name} (ID: $locationId)")
        return salesDataRepository.save(salesDataWithLocation)
    }
    
    @DeleteMapping("/{id}")
    fun deleteSalesData(
        @PathVariable id: Long,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ) {
        val locationId = getLocationId(locationIdHeader)
        val salesData = salesDataRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Sales data not found")
        }
        
        // Security check: ensure the sales data belongs to the requested location
        if (salesData.location?.id != locationId) {
            logger.warn("Attempted to delete sales data from different location. Data location: ${salesData.location?.id}, Requested location: $locationId")
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete sales data from different location")
        }
        
        salesDataRepository.deleteById(id)
        logger.info("Deleted sales data ID: $id for location: $locationId")
    }
    
    @DeleteMapping
    fun deleteAllSalesData(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ) {
        val locationId = getLocationId(locationIdHeader)
        
        // Validate that the location exists
        locationRepository.findById(locationId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found")
        }
        
        salesDataRepository.deleteByLocationId(locationId)
        logger.info("Deleted all sales data for location: $locationId")
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