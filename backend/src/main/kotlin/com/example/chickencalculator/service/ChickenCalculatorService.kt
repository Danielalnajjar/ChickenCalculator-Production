package com.example.chickencalculator.service

import com.example.chickencalculator.model.CalculationResult
import com.example.chickencalculator.model.MarinationRequest
import com.example.chickencalculator.repository.SalesDataRepository
import com.example.chickencalculator.utils.ChickenCalculator
import org.springframework.stereotype.Service

@Service
class ChickenCalculatorService(
    private val salesDataRepository: SalesDataRepository
) {
    
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
            if (request.alreadyMarinatedSoy > 0 || 
                request.alreadyMarinatedTeriyaki > 0 || 
                request.alreadyMarinatedTurmeric > 0) {
                // Account for already-marinated chicken (end-of-day scenario)
                val totalNeeded = ChickenCalculator.calculateMarination(
                    request.inventory, request.projectedSales, salesTotals
                )
                
                // Subtract already marinated amounts
                val alreadyMarinatedSoyGrams = request.alreadyMarinatedSoy * 1000.0
                val alreadyMarinatedTeriyakiGrams = request.alreadyMarinatedTeriyaki * 1000.0
                val alreadyMarinatedTurmericGrams = request.alreadyMarinatedTurmeric * 1000.0
                
                CalculationResult(
                    rawToMarinateSoy = maxOf(0.0, totalNeeded.rawToMarinateSoy - alreadyMarinatedSoyGrams),
                    rawToMarinateTeriyaki = maxOf(0.0, totalNeeded.rawToMarinateTeriyaki - alreadyMarinatedTeriyakiGrams),
                    rawToMarinateTurmeric = maxOf(0.0, totalNeeded.rawToMarinateTurmeric - alreadyMarinatedTurmericGrams),
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
    
    fun hasSalesData(): Boolean {
        val totals = salesDataRepository.getSalesTotals()
        return totals.totalSales > 0
    }
}