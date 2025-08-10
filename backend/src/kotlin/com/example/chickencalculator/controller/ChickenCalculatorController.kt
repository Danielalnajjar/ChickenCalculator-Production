package com.example.chickencalculator.controller

import com.example.chickencalculator.model.CalculationResult
import com.example.chickencalculator.model.MarinationRequest
import com.example.chickencalculator.service.ChickenCalculatorService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/calculator")
@CrossOrigin(origins = ["http://localhost:3000"])
class ChickenCalculatorController(
    private val calculatorService: ChickenCalculatorService
) {
    
    @PostMapping("/calculate")
    fun calculateMarination(@RequestBody request: MarinationRequest): CalculationResult {
        return calculatorService.calculateMarination(request)
    }
    
    @GetMapping("/has-sales-data")
    fun hasSalesData(): Map<String, Boolean> {
        return mapOf("hasSalesData" to calculatorService.hasSalesData())
    }
}