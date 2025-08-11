package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.SalesTotals
import com.example.chickencalculator.repository.SalesDataRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sales-data")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"]
)
class SalesDataController(
    private val salesDataRepository: SalesDataRepository
) {
    
    @GetMapping
    fun getAllSalesData(): List<SalesData> {
        return salesDataRepository.findAllByOrderByDateDesc()
    }
    
    @GetMapping("/totals")
    fun getSalesTotals(): SalesTotals {
        return salesDataRepository.getSalesTotals()
    }
    
    @PostMapping
    fun addSalesData(@RequestBody salesData: SalesData): SalesData {
        return salesDataRepository.save(salesData)
    }
    
    @DeleteMapping("/{id}")
    fun deleteSalesData(@PathVariable id: Long) {
        salesDataRepository.deleteById(id)
    }
    
    @DeleteMapping
    fun deleteAllSalesData() {
        salesDataRepository.deleteAll()
    }
}