package com.example.chickencalculator.controller

import com.example.chickencalculator.config.ApiVersionConfig
import com.example.chickencalculator.model.CalculationResult
import com.example.chickencalculator.model.MarinationRequest
import com.example.chickencalculator.service.ChickenCalculatorService
import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${ApiVersionConfig.API_VERSION}/calculator")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"]
)
class ChickenCalculatorController(
    private val calculatorService: ChickenCalculatorService,
    private val metricsService: MetricsService
) {
    
    @PostMapping("/calculate")
    @Timed(value = "chicken.calculator.calculate.time", description = "Time taken for chicken calculations")
    fun calculateMarination(
        @RequestBody request: MarinationRequest,
        httpRequest: HttpServletRequest
    ): CalculationResult {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val result = calculatorService.calculateMarination(request)
            val processingTime = System.currentTimeMillis() - startTime
            
            // Record metrics
            metricsService.recordCalculation(
                locationSlug = locationSlug,
                weight = request.availableRawChickenKg?.toDouble() ?: 0.0,
                processingTimeMs = processingTime
            )
            
            result
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordError("calculation", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
    
    @GetMapping("/has-sales-data")
    @Timed(value = "chicken.calculator.has_sales_data.time", description = "Time taken to check sales data")
    fun hasSalesData(httpRequest: HttpServletRequest): Map<String, Boolean> {
        val startTime = System.currentTimeMillis()
        val locationSlug = httpRequest.getHeader("X-Location-Slug")
        
        return try {
            val result = mapOf("hasSalesData" to calculatorService.hasSalesData())
            val processingTime = System.currentTimeMillis() - startTime
            
            // Record metrics
            metricsService.recordSalesDataOperation(
                locationSlug = locationSlug,
                operation = "check"
            )
            metricsService.recordDatabaseOperation("sales_data_check", processingTime)
            
            result
        } catch (e: Exception) {
            metricsService.recordError("sales_data_check", e.javaClass.simpleName, locationSlug)
            throw e
        }
    }
}