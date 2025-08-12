package com.example.chickencalculator.service

import com.example.chickencalculator.model.CalculationResult
import com.example.chickencalculator.model.MarinationRequest
import com.example.chickencalculator.repository.SalesDataRepository
import com.example.chickencalculator.utils.ChickenCalculator
import com.example.chickencalculator.utils.max
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ChickenCalculatorService(
    private val salesDataRepository: SalesDataRepository
) {
    
    @Transactional(readOnly = true)
    fun calculateMarination(request: MarinationRequest): CalculationResult {
        val salesTotals = salesDataRepository.getSalesTotals()
        
        return if (request.availableRawChickenKg != null) {
            // Limited raw chicken distribution
            ChickenCalculator.distributeRawChicken(
                inventory = request.inventory,
                sales = request.projectedSales,
                totals = salesTotals,
                availableRawChickenKg = request.availableRawChickenKg,
                alreadyMarinatedSoy = request.alreadyMarinatedSoy,
                alreadyMarinatedTeriyaki = request.alreadyMarinatedTeriyaki,
                alreadyMarinatedTurmeric = request.alreadyMarinatedTurmeric
            )
        } else {
            // Standard calculation
            if (request.alreadyMarinatedSoy > BigDecimal.ZERO || 
                request.alreadyMarinatedTeriyaki > BigDecimal.ZERO || 
                request.alreadyMarinatedTurmeric > BigDecimal.ZERO) {
                // Account for already-marinated chicken (end-of-day scenario)
                val totalNeeded = ChickenCalculator.calculateMarination(
                    request.inventory, request.projectedSales, salesTotals
                )
                
                // Subtract already marinated amounts
                val thousand = BigDecimal.valueOf(1000)
                val alreadyMarinatedSoyGrams = request.alreadyMarinatedSoy.multiply(thousand)
                val alreadyMarinatedTeriyakiGrams = request.alreadyMarinatedTeriyaki.multiply(thousand)
                val alreadyMarinatedTurmericGrams = request.alreadyMarinatedTurmeric.multiply(thousand)
                
                CalculationResult(
                    rawToMarinateSoy = totalNeeded.rawToMarinateSoy.subtract(alreadyMarinatedSoyGrams).max(BigDecimal.ZERO),
                    rawToMarinateTeriyaki = totalNeeded.rawToMarinateTeriyaki.subtract(alreadyMarinatedTeriyakiGrams).max(BigDecimal.ZERO),
                    rawToMarinateTurmeric = totalNeeded.rawToMarinateTurmeric.subtract(alreadyMarinatedTurmericGrams).max(BigDecimal.ZERO),
                    portionsPer1000Soy = totalNeeded.portionsPer1000Soy,
                    portionsPer1000Teriyaki = totalNeeded.portionsPer1000Teriyaki,
                    portionsPer1000Turmeric = totalNeeded.portionsPer1000Turmeric
                )
            } else {
                // Standard calculation
                ChickenCalculator.calculateMarination(
                    request.inventory, request.projectedSales, salesTotals
                )
            }
        }
    }
    
    @Transactional(readOnly = true)
    fun hasSalesData(): Boolean {
        val totals = salesDataRepository.getSalesTotals()
        return totals.totalSales > BigDecimal.ZERO
    }
}