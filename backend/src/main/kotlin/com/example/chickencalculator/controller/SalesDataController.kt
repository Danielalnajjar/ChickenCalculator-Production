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
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"]
)
class SalesDataController(
    private val salesDataRepository: SalesDataRepository,
    private val locationRepository: LocationRepository
) {
    
    private val logger = LoggerFactory.getLogger(SalesDataController::class.java)
    
    @GetMapping
    fun getAllSalesData(): List<SalesData> {
        // Return sales data for default location only (public access)
        val defaultLocation = locationRepository.findByIsDefaultTrue()
        return if (defaultLocation != null) {
            salesDataRepository.findByLocationIdOrderByDateDesc(defaultLocation.id)
        } else {
            emptyList()
        }
    }
    
    @GetMapping("/totals")
    fun getSalesTotals(): SalesTotals {
        // Return totals for default location only
        val defaultLocation = locationRepository.findByIsDefaultTrue()
        return if (defaultLocation != null) {
            salesDataRepository.getSalesTotalsByLocation(defaultLocation.id)
        } else {
            SalesTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }
    
    @PostMapping
    fun addSalesData(@RequestBody salesData: SalesData): SalesData {
        // Auto-assign default location if not provided
        val defaultLocation = locationRepository.findByIsDefaultTrue()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default location not found")
        
        // Create new sales data with default location
        val salesDataWithLocation = salesData.copy(location = defaultLocation)
        
        logger.info("Adding sales data for default location: ${defaultLocation.name}")
        return salesDataRepository.save(salesDataWithLocation)
    }
    
    @DeleteMapping("/{id}")
    fun deleteSalesData(@PathVariable id: Long) {
        // Only allow deletion of sales data from default location
        val defaultLocation = locationRepository.findByIsDefaultTrue()
        if (defaultLocation != null) {
            val salesData = salesDataRepository.findById(id).orElse(null)
            if (salesData?.location?.id == defaultLocation.id) {
                salesDataRepository.deleteById(id)
            }
        }
    }
    
    @DeleteMapping
    fun deleteAllSalesData() {
        // Only delete sales data for default location
        val defaultLocation = locationRepository.findByIsDefaultTrue()
        if (defaultLocation != null) {
            salesDataRepository.deleteByLocationId(defaultLocation.id)
        }
    }
}