package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.MarinationLog
import com.example.chickencalculator.repository.MarinationLogRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/marination-log")
@CrossOrigin(origins = ["http://localhost:3000"])
class MarinationLogController(
    private val marinationLogRepository: MarinationLogRepository
) {
    
    @GetMapping
    fun getAllMarinationLogs(): List<MarinationLog> {
        return marinationLogRepository.findAllByOrderByTimestampDesc()
    }
    
    @GetMapping("/today")
    fun getTodaysMarinationLogs(): List<MarinationLog> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.plusDays(1).atStartOfDay()
        return marinationLogRepository.findTodaysMarinationLogs(startOfDay, endOfDay)
    }
    
    @PostMapping
    fun addMarinationLog(@RequestBody log: MarinationLog): MarinationLog {
        return marinationLogRepository.save(log)
    }
    
    @DeleteMapping("/{id}")
    fun deleteMarinationLog(@PathVariable id: Long) {
        marinationLogRepository.deleteById(id)
    }
}