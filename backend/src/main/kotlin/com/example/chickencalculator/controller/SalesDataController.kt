package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.SalesTotals
import com.example.chickencalculator.service.SalesDataService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sales-data")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With", "X-Location-Id"]
)
class SalesDataController(
    private val salesDataService: SalesDataService
) {
    
    private val logger = LoggerFactory.getLogger(SalesDataController::class.java)
    
    @GetMapping
    fun getAllSalesData(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): List<SalesData> {
        val locationId = salesDataService.resolveLocationId(locationIdHeader)
        return salesDataService.getAllSalesDataByLocation(locationId)
    }
    
    @GetMapping("/totals")
    fun getSalesTotals(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): SalesTotals {
        val locationId = salesDataService.resolveLocationId(locationIdHeader)
        return salesDataService.getSalesTotalsByLocation(locationId)
    }
    
    @PostMapping
    fun addSalesData(
        @RequestBody salesData: SalesData,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ): SalesData {
        val locationId = salesDataService.resolveLocationId(locationIdHeader)
        return salesDataService.addSalesData(salesData, locationId)
    }
    
    @DeleteMapping("/{id}")
    fun deleteSalesData(
        @PathVariable id: Long,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ) {
        val locationId = salesDataService.resolveLocationId(locationIdHeader)
        salesDataService.deleteSalesData(id, locationId)
    }
    
    @DeleteMapping
    fun deleteAllSalesData(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?
    ) {
        val locationId = salesDataService.resolveLocationId(locationIdHeader)
        salesDataService.deleteAllSalesDataByLocation(locationId)
    }
}