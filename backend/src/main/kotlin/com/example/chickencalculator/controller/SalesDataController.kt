package com.example.chickencalculator.controller

import com.example.chickencalculator.config.ApiVersionConfig
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.SalesTotals
import com.example.chickencalculator.service.SalesDataService
import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${ApiVersionConfig.API_VERSION}/sales-data")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With", "X-Location-Id"]
)
class SalesDataController(
    private val salesDataService: SalesDataService,
    private val metricsService: MetricsService
) {
    
    private val logger = LoggerFactory.getLogger(SalesDataController::class.java)
    
    @GetMapping
    @Timed(value = "chicken.calculator.sales_data.get_all.time", description = "Time taken to get all sales data")
    fun getAllSalesData(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ): List<SalesData> {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val locationId = salesDataService.resolveLocationId(locationIdHeader)
            val result = salesDataService.getAllSalesDataByLocation(locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordSalesDataOperation(locationSlug, "get_all", result.size)
            metricsService.recordDatabaseOperation("sales_data_get_all", processingTime)
            
            result
        } catch (e: Exception) {
            metricsService.recordError("sales_data_get_all", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @GetMapping("/totals")
    @Timed(value = "chicken.calculator.sales_data.get_totals.time", description = "Time taken to get sales totals")
    fun getSalesTotals(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ): SalesTotals {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val locationId = salesDataService.resolveLocationId(locationIdHeader)
            val result = salesDataService.getSalesTotalsByLocation(locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordSalesDataOperation(locationSlug, "get_totals")
            metricsService.recordDatabaseOperation("sales_data_get_totals", processingTime)
            
            result
        } catch (e: Exception) {
            metricsService.recordError("sales_data_get_totals", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @PostMapping
    @Timed(value = "chicken.calculator.sales_data.add.time", description = "Time taken to add sales data")
    fun addSalesData(
        @RequestBody salesData: SalesData,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ): SalesData {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val locationId = salesDataService.resolveLocationId(locationIdHeader)
            val result = salesDataService.addSalesData(salesData, locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordSalesDataOperation(locationSlug, "create", 1)
            metricsService.recordDatabaseOperation("sales_data_add", processingTime)
            
            result
        } catch (e: Exception) {
            metricsService.recordError("sales_data_add", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @DeleteMapping("/{id}")
    @Timed(value = "chicken.calculator.sales_data.delete.time", description = "Time taken to delete sales data")
    fun deleteSalesData(
        @PathVariable id: Long,
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ) {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        try {
            val locationId = salesDataService.resolveLocationId(locationIdHeader)
            salesDataService.deleteSalesData(id, locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordSalesDataOperation(locationSlug, "delete", 1)
            metricsService.recordDatabaseOperation("sales_data_delete", processingTime)
        } catch (e: Exception) {
            metricsService.recordError("sales_data_delete", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @DeleteMapping
    @Timed(value = "chicken.calculator.sales_data.delete_all.time", description = "Time taken to delete all sales data")
    fun deleteAllSalesData(
        @RequestHeader("X-Location-Id", required = false) locationIdHeader: String?,
        httpRequest: HttpServletRequest
    ) {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        try {
            val locationId = salesDataService.resolveLocationId(locationIdHeader)
            salesDataService.deleteAllSalesDataByLocation(locationId)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordSalesDataOperation(locationSlug, "delete_all")
            metricsService.recordDatabaseOperation("sales_data_delete_all", processingTime)
        } catch (e: Exception) {
            metricsService.recordError("sales_data_delete_all", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
}