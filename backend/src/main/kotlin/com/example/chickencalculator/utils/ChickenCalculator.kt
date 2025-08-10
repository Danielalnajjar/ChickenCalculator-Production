package com.example.chickencalculator.utils

import com.example.chickencalculator.model.CalculationResult
import com.example.chickencalculator.model.InventoryData
import com.example.chickencalculator.model.ProjectedSales
import com.example.chickencalculator.model.SalesTotals
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Utility object for chicken inventory calculations
 *
 * This calculator determines how much raw chicken to marinate today to ensure
 * adequate supply through a 4-day forecast period.
 */
object ChickenCalculator {

    /**
     * Calculate the amount of raw chicken to marinate today
     */
    fun calculateMarination(
        inventory: InventoryData,
        sales: ProjectedSales,
        totals: SalesTotals,
        safetyFactor: Double = 0.0
    ): CalculationResult {

        // Calculate portions per $1000 for each chicken type based on historical data
        val portionsPer1000 = calculatePortionsPer1000(totals)

        // Calculate projected grams needed per day for each chicken type
        val projectedGrams = calculateProjectedGramsPerDay(sales, portionsPer1000, safetyFactor)

        // Calculate raw chicken to marinate for each type
        val rawToMarinateSoy = calculateRawToMarinate(
            currentGrams = inventory.pansSoy * ChickenConstants.GRAMS_PER_PAN_SOY,
            projectedGrams = projectedGrams.soy,
            yieldFactor = ChickenConstants.YIELD_FACTOR_SOY,
            gramsPerPan = ChickenConstants.GRAMS_PER_PAN_SOY
        )

        val rawToMarinateTeriyaki = calculateRawToMarinate(
            currentGrams = inventory.pansTeriyaki * ChickenConstants.GRAMS_PER_PAN_TERIYAKI,
            projectedGrams = projectedGrams.teriyaki,
            yieldFactor = ChickenConstants.YIELD_FACTOR_TERIYAKI,
            gramsPerPan = ChickenConstants.GRAMS_PER_PAN_TERIYAKI
        )

        val rawToMarinateTurmeric = calculateRawToMarinate(
            currentGrams = inventory.pansTurmeric * ChickenConstants.GRAMS_PER_PAN_TURMERIC,
            projectedGrams = projectedGrams.turmeric,
            yieldFactor = ChickenConstants.YIELD_FACTOR_TURMERIC,
            gramsPerPan = ChickenConstants.GRAMS_PER_PAN_TURMERIC
        )

        return CalculationResult(
            rawToMarinateSoy = rawToMarinateSoy,
            rawToMarinateTeriyaki = rawToMarinateTeriyaki,
            rawToMarinateTurmeric = rawToMarinateTurmeric,
            portionsPer1000Soy = portionsPer1000.soy,
            portionsPer1000Teriyaki = portionsPer1000.teriyaki,
            portionsPer1000Turmeric = portionsPer1000.turmeric
        )
    }

    /**
     * Calculate portions per $1000 based on historical sales
     */
    private fun calculatePortionsPer1000(totals: SalesTotals): PortionsPerThousand {
        var soy = 0.0
        var teriyaki = 0.0
        var turmeric = 0.0

        if (totals.totalSales > 0) {
            soy = (totals.totalPortionsSoy / totals.totalSales) * 1000
            teriyaki = (totals.totalPortionsTeriyaki / totals.totalSales) * 1000
            turmeric = (totals.totalPortionsTurmeric / totals.totalSales) * 1000
        }

        return PortionsPerThousand(soy, teriyaki, turmeric)
    }

    /**
     * Calculate projected grams needed per day for each chicken type
     */
    private fun calculateProjectedGramsPerDay(
        sales: ProjectedSales,
        portionsPer1000: PortionsPerThousand,
        safetyFactor: Double
    ): ProjectedGrams {
        val soy = DoubleArray(ChickenConstants.FORECAST_DAYS)
        val teriyaki = DoubleArray(ChickenConstants.FORECAST_DAYS)
        val turmeric = DoubleArray(ChickenConstants.FORECAST_DAYS)

        for (i in 0 until ChickenConstants.FORECAST_DAYS) {
            val dailySales = sales.getSales(i)
            val salesPer1000 = dailySales / 1000

            soy[i] = salesPer1000 * portionsPer1000.soy * ChickenConstants.PORTION_SIZE_SOY * (1 + safetyFactor)
            teriyaki[i] = salesPer1000 * portionsPer1000.teriyaki * ChickenConstants.PORTION_SIZE_TERIYAKI * (1 + safetyFactor)
            turmeric[i] = salesPer1000 * portionsPer1000.turmeric * ChickenConstants.PORTION_SIZE_TURMERIC * (1 + safetyFactor)
        }

        return ProjectedGrams(soy, teriyaki, turmeric)
    }

    /**
     * Calculate the raw amount to marinate for a specific chicken type
     */
    private fun calculateRawToMarinate(
        currentGrams: Double,
        projectedGrams: DoubleArray,
        yieldFactor: Double,
        gramsPerPan: Double
    ): Double {
        // Calculate total needs for the entire 4-day period (Days 0-3)
        val totalNeeds = projectedGrams[0] + projectedGrams[1] + projectedGrams[2] + projectedGrams[3]

        // Calculate shortfall for the entire period
        val totalShortfall = if (currentGrams < totalNeeds) {
            totalNeeds - currentGrams
        } else {
            0.0
        }

        // If we have a shortfall for the 4-day period, marinate enough to cover it
        val rawToMarinate = if (totalShortfall > 0) {
            totalShortfall / yieldFactor
        } else {
            0.0
        }

        // Convert raw grams to pans for practical kitchen use
        return roundToPans(rawToMarinate, gramsPerPan / yieldFactor)
    }

    /**
     * Round raw grams to practical pan quantities
     */
    private fun roundToPans(rawGrams: Double, rawGramsPerPan: Double): Double {
        // Calculate how many pans are needed
        val rawPansNeeded = rawGrams / rawGramsPerPan

        // Get the fractional part
        val fractionalPart = rawPansNeeded - floor(rawPansNeeded)

        // Round up if 30% or more of a pan, otherwise round down
        val roundedPans = if (fractionalPart >= 0.3) {
            ceil(rawPansNeeded)
        } else {
            floor(rawPansNeeded)
        }

        // Convert back to grams
        return roundedPans * rawGramsPerPan
    }

    /**
     * Calculate how to distribute limited raw chicken
     */
    fun distributeRawChicken(
        inventory: InventoryData,
        sales: ProjectedSales,
        totals: SalesTotals,
        availableRawChickenKg: Double,
        alreadyMarinatedSoy: Double = 0.0,
        alreadyMarinatedTeriyaki: Double = 0.0,
        alreadyMarinatedTurmeric: Double = 0.0,
        safetyFactor: Double = 0.0
    ): CalculationResult {
        // First calculate the ideal amounts needed
        val idealResult = calculateMarination(inventory, sales, totals, safetyFactor)

        // Convert already-marinated amounts from kg to grams
        val alreadyMarinatedSoyGrams = alreadyMarinatedSoy * 1000.0
        val alreadyMarinatedTeriyakiGrams = alreadyMarinatedTeriyaki * 1000.0
        val alreadyMarinatedTurmericGrams = alreadyMarinatedTurmeric * 1000.0

        // Subtract already-marinated amounts from the ideal needed amounts
        val soyNeeded = max(0.0, idealResult.rawToMarinateSoy - alreadyMarinatedSoyGrams)
        val teriyakiNeeded = max(0.0, idealResult.rawToMarinateTeriyaki - alreadyMarinatedTeriyakiGrams)
        val turmericNeeded = max(0.0, idealResult.rawToMarinateTurmeric - alreadyMarinatedTurmericGrams)

        // Calculate total needed raw chicken
        val totalNeededRaw = soyNeeded + teriyakiNeeded + turmericNeeded

        // If available chicken exceeds what's needed, return adjusted amounts
        if (availableRawChickenKg * 1000.0 >= totalNeededRaw) {
            return CalculationResult(
                rawToMarinateSoy = soyNeeded,
                rawToMarinateTeriyaki = teriyakiNeeded,
                rawToMarinateTurmeric = turmericNeeded,
                portionsPer1000Soy = idealResult.portionsPer1000Soy,
                portionsPer1000Teriyaki = idealResult.portionsPer1000Teriyaki,
                portionsPer1000Turmeric = idealResult.portionsPer1000Turmeric
            )
        }

        // Calculate portions per $1000 for each chicken type
        val portionsPer1000 = calculatePortionsPer1000(totals)

        // Calculate projected grams needed per day for each chicken type
        val projectedGrams = calculateProjectedGramsPerDay(sales, portionsPer1000, safetyFactor)

        // Convert kg to grams for available chicken
        var availableRawChicken = availableRawChickenKg * 1000.0

        // Calculate current inventory in grams
        val soyInventory = inventory.pansSoy * ChickenConstants.GRAMS_PER_PAN_SOY
        val teriyakiInventory = inventory.pansTeriyaki * ChickenConstants.GRAMS_PER_PAN_TERIYAKI
        val turmericInventory = inventory.pansTurmeric * ChickenConstants.GRAMS_PER_PAN_TURMERIC

        // Calculate total 4-day needs for each type
        val soyTotalNeeds = projectedGrams.soy[0] + projectedGrams.soy[1] + projectedGrams.soy[2] + projectedGrams.soy[3]
        val teriyakiTotalNeeds = projectedGrams.teriyaki[0] + projectedGrams.teriyaki[1] + projectedGrams.teriyaki[2] + projectedGrams.teriyaki[3]
        val turmericTotalNeeds = projectedGrams.turmeric[0] + projectedGrams.turmeric[1] + projectedGrams.turmeric[2] + projectedGrams.turmeric[3]

        // Calculate shortfalls for the full 4-day period
        val soyShortfall = max(0.0, soyTotalNeeds - soyInventory) / ChickenConstants.YIELD_FACTOR_SOY
        val teriyakiShortfall = max(0.0, teriyakiTotalNeeds - teriyakiInventory) / ChickenConstants.YIELD_FACTOR_TERIYAKI
        val turmericShortfall = max(0.0, turmericTotalNeeds - turmericInventory) / ChickenConstants.YIELD_FACTOR_TURMERIC

        val totalShortfall = soyShortfall + teriyakiShortfall + turmericShortfall

        // Distribute available chicken proportionally
        var distributedSoy = 0.0
        var distributedTeriyaki = 0.0
        var distributedTurmeric = 0.0

        if (totalShortfall > 0 && availableRawChicken > 0) {
            if (totalShortfall > availableRawChicken) {
                // Can't cover all needs, distribute proportionally
                val ratio = availableRawChicken / totalShortfall
                distributedSoy = soyShortfall * ratio
                distributedTeriyaki = teriyakiShortfall * ratio
                distributedTurmeric = turmericShortfall * ratio
            } else {
                // Cover all needs
                distributedSoy = soyShortfall
                distributedTeriyaki = teriyakiShortfall
                distributedTurmeric = turmericShortfall
            }
        }

        return CalculationResult(
            rawToMarinateSoy = distributedSoy,
            rawToMarinateTeriyaki = distributedTeriyaki,
            rawToMarinateTurmeric = distributedTurmeric,
            portionsPer1000Soy = portionsPer1000.soy,
            portionsPer1000Teriyaki = portionsPer1000.teriyaki,
            portionsPer1000Turmeric = portionsPer1000.turmeric
        )
    }

    /**
     * Helper data class for portions per $1000 calculations
     */
    private data class PortionsPerThousand(
        val soy: Double,
        val teriyaki: Double,
        val turmeric: Double
    )

    /**
     * Helper data class for projected grams calculations
     */
    private data class ProjectedGrams(
        val soy: DoubleArray,
        val teriyaki: DoubleArray,
        val turmeric: DoubleArray
    )
}