package com.example.chickencalculator.utils

import com.example.chickencalculator.model.CalculationResult
import com.example.chickencalculator.model.InventoryData
import com.example.chickencalculator.model.ProjectedSales
import com.example.chickencalculator.model.SalesTotals
import java.math.BigDecimal
import java.math.RoundingMode

// Extension function for BigDecimal max operation
fun BigDecimal.max(other: BigDecimal): BigDecimal = if (this > other) this else other

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
        safetyFactor: BigDecimal = BigDecimal.ZERO
    ): CalculationResult {

        // Calculate portions per $1000 for each chicken type based on historical data
        val portionsPer1000 = calculatePortionsPer1000(totals)

        // Calculate projected grams needed per day for each chicken type
        val projectedGrams = calculateProjectedGramsPerDay(sales, portionsPer1000, safetyFactor)

        // Calculate raw chicken to marinate for each type
        val rawToMarinateSoy = calculateRawToMarinate(
            currentGrams = inventory.pansSoy.multiply(BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_SOY)),
            projectedGrams = projectedGrams.soy,
            yieldFactor = BigDecimal.valueOf(ChickenConstants.YIELD_FACTOR_SOY),
            gramsPerPan = BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_SOY)
        )

        val rawToMarinateTeriyaki = calculateRawToMarinate(
            currentGrams = inventory.pansTeriyaki.multiply(BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_TERIYAKI)),
            projectedGrams = projectedGrams.teriyaki,
            yieldFactor = BigDecimal.valueOf(ChickenConstants.YIELD_FACTOR_TERIYAKI),
            gramsPerPan = BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_TERIYAKI)
        )

        val rawToMarinateTurmeric = calculateRawToMarinate(
            currentGrams = inventory.pansTurmeric.multiply(BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_TURMERIC)),
            projectedGrams = projectedGrams.turmeric,
            yieldFactor = BigDecimal.valueOf(ChickenConstants.YIELD_FACTOR_TURMERIC),
            gramsPerPan = BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_TURMERIC)
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
        var soy = BigDecimal.ZERO
        var teriyaki = BigDecimal.ZERO
        var turmeric = BigDecimal.ZERO

        if (totals.totalSales > BigDecimal.ZERO) {
            val thousand = BigDecimal.valueOf(1000)
            soy = totals.totalPortionsSoy.divide(totals.totalSales, 10, RoundingMode.HALF_UP).multiply(thousand)
            teriyaki = totals.totalPortionsTeriyaki.divide(totals.totalSales, 10, RoundingMode.HALF_UP).multiply(thousand)
            turmeric = totals.totalPortionsTurmeric.divide(totals.totalSales, 10, RoundingMode.HALF_UP).multiply(thousand)
        }

        return PortionsPerThousand(soy, teriyaki, turmeric)
    }

    /**
     * Calculate projected grams needed per day for each chicken type
     */
    private fun calculateProjectedGramsPerDay(
        sales: ProjectedSales,
        portionsPer1000: PortionsPerThousand,
        safetyFactor: BigDecimal
    ): ProjectedGrams {
        val soy = Array(ChickenConstants.FORECAST_DAYS) { BigDecimal.ZERO }
        val teriyaki = Array(ChickenConstants.FORECAST_DAYS) { BigDecimal.ZERO }
        val turmeric = Array(ChickenConstants.FORECAST_DAYS) { BigDecimal.ZERO }

        val thousand = BigDecimal.valueOf(1000)
        val one = BigDecimal.ONE
        val safetyMultiplier = one.add(safetyFactor)

        for (i in 0 until ChickenConstants.FORECAST_DAYS) {
            val dailySales = sales.getSales(i)
            val salesPer1000 = dailySales.divide(thousand, 10, RoundingMode.HALF_UP)

            soy[i] = salesPer1000.multiply(portionsPer1000.soy)
                .multiply(BigDecimal.valueOf(ChickenConstants.PORTION_SIZE_SOY.toLong()))
                .multiply(safetyMultiplier)
            teriyaki[i] = salesPer1000.multiply(portionsPer1000.teriyaki)
                .multiply(BigDecimal.valueOf(ChickenConstants.PORTION_SIZE_TERIYAKI))
                .multiply(safetyMultiplier)
            turmeric[i] = salesPer1000.multiply(portionsPer1000.turmeric)
                .multiply(BigDecimal.valueOf(ChickenConstants.PORTION_SIZE_TURMERIC))
                .multiply(safetyMultiplier)
        }

        return ProjectedGrams(soy, teriyaki, turmeric)
    }

    /**
     * Calculate the raw amount to marinate for a specific chicken type
     */
    private fun calculateRawToMarinate(
        currentGrams: BigDecimal,
        projectedGrams: Array<BigDecimal>,
        yieldFactor: BigDecimal,
        gramsPerPan: BigDecimal
    ): BigDecimal {
        // Calculate total needs for the entire 4-day period (Days 0-3)
        val totalNeeds = projectedGrams[0].add(projectedGrams[1]).add(projectedGrams[2]).add(projectedGrams[3])

        // Calculate shortfall for the entire period
        val totalShortfall = if (currentGrams < totalNeeds) {
            totalNeeds.subtract(currentGrams)
        } else {
            BigDecimal.ZERO
        }

        // If we have a shortfall for the 4-day period, marinate enough to cover it
        val rawToMarinate = if (totalShortfall > BigDecimal.ZERO) {
            totalShortfall.divide(yieldFactor, 10, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // Convert raw grams to pans for practical kitchen use
        return roundToPans(rawToMarinate, gramsPerPan.divide(yieldFactor, 10, RoundingMode.HALF_UP))
    }

    /**
     * Round raw grams to practical pan quantities
     */
    private fun roundToPans(rawGrams: BigDecimal, rawGramsPerPan: BigDecimal): BigDecimal {
        // Calculate how many pans are needed
        val rawPansNeeded = rawGrams.divide(rawGramsPerPan, 10, RoundingMode.HALF_UP)

        // Get the integer and fractional parts
        val integerPart = rawPansNeeded.setScale(0, RoundingMode.DOWN)
        val fractionalPart = rawPansNeeded.subtract(integerPart)

        // Round up if 30% or more of a pan, otherwise round down
        val roundedPans = if (fractionalPart >= BigDecimal.valueOf(0.3)) {
            rawPansNeeded.setScale(0, RoundingMode.CEILING)
        } else {
            rawPansNeeded.setScale(0, RoundingMode.DOWN)
        }

        // Convert back to grams
        return roundedPans.multiply(rawGramsPerPan)
    }

    /**
     * Calculate how to distribute limited raw chicken
     */
    fun distributeRawChicken(
        inventory: InventoryData,
        sales: ProjectedSales,
        totals: SalesTotals,
        availableRawChickenKg: BigDecimal,
        alreadyMarinatedSoy: BigDecimal = BigDecimal.ZERO,
        alreadyMarinatedTeriyaki: BigDecimal = BigDecimal.ZERO,
        alreadyMarinatedTurmeric: BigDecimal = BigDecimal.ZERO,
        safetyFactor: BigDecimal = BigDecimal.ZERO
    ): CalculationResult {
        // First calculate the ideal amounts needed
        val idealResult = calculateMarination(inventory, sales, totals, safetyFactor)

        // Convert already-marinated amounts from kg to grams
        val thousand = BigDecimal.valueOf(1000)
        val alreadyMarinatedSoyGrams = alreadyMarinatedSoy.multiply(thousand)
        val alreadyMarinatedTeriyakiGrams = alreadyMarinatedTeriyaki.multiply(thousand)
        val alreadyMarinatedTurmericGrams = alreadyMarinatedTurmeric.multiply(thousand)

        // Subtract already-marinated amounts from the ideal needed amounts
        val soyNeeded = idealResult.rawToMarinateSoy.subtract(alreadyMarinatedSoyGrams).max(BigDecimal.ZERO)
        val teriyakiNeeded = idealResult.rawToMarinateTeriyaki.subtract(alreadyMarinatedTeriyakiGrams).max(BigDecimal.ZERO)
        val turmericNeeded = idealResult.rawToMarinateTurmeric.subtract(alreadyMarinatedTurmericGrams).max(BigDecimal.ZERO)

        // Calculate total needed raw chicken
        val totalNeededRaw = soyNeeded.add(teriyakiNeeded).add(turmericNeeded)

        // If available chicken exceeds what's needed, return adjusted amounts
        if (availableRawChickenKg.multiply(thousand) >= totalNeededRaw) {
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
        var availableRawChicken = availableRawChickenKg.multiply(thousand)

        // Calculate current inventory in grams
        val soyInventory = inventory.pansSoy.multiply(BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_SOY))
        val teriyakiInventory = inventory.pansTeriyaki.multiply(BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_TERIYAKI))
        val turmericInventory = inventory.pansTurmeric.multiply(BigDecimal.valueOf(ChickenConstants.GRAMS_PER_PAN_TURMERIC))

        // Calculate total 4-day needs for each type
        val soyTotalNeeds = projectedGrams.soy[0].add(projectedGrams.soy[1]).add(projectedGrams.soy[2]).add(projectedGrams.soy[3])
        val teriyakiTotalNeeds = projectedGrams.teriyaki[0].add(projectedGrams.teriyaki[1]).add(projectedGrams.teriyaki[2]).add(projectedGrams.teriyaki[3])
        val turmericTotalNeeds = projectedGrams.turmeric[0].add(projectedGrams.turmeric[1]).add(projectedGrams.turmeric[2]).add(projectedGrams.turmeric[3])

        // Calculate shortfalls for the full 4-day period
        val soyShortfall = soyTotalNeeds.subtract(soyInventory).max(BigDecimal.ZERO)
            .divide(BigDecimal.valueOf(ChickenConstants.YIELD_FACTOR_SOY), 10, RoundingMode.HALF_UP)
        val teriyakiShortfall = teriyakiTotalNeeds.subtract(teriyakiInventory).max(BigDecimal.ZERO)
            .divide(BigDecimal.valueOf(ChickenConstants.YIELD_FACTOR_TERIYAKI), 10, RoundingMode.HALF_UP)
        val turmericShortfall = turmericTotalNeeds.subtract(turmericInventory).max(BigDecimal.ZERO)
            .divide(BigDecimal.valueOf(ChickenConstants.YIELD_FACTOR_TURMERIC), 10, RoundingMode.HALF_UP)

        val totalShortfall = soyShortfall.add(teriyakiShortfall).add(turmericShortfall)

        // Distribute available chicken proportionally
        var distributedSoy = BigDecimal.ZERO
        var distributedTeriyaki = BigDecimal.ZERO
        var distributedTurmeric = BigDecimal.ZERO

        if (totalShortfall > BigDecimal.ZERO && availableRawChicken > BigDecimal.ZERO) {
            if (totalShortfall > availableRawChicken) {
                // Can't cover all needs, distribute proportionally
                val ratio = availableRawChicken.divide(totalShortfall, 10, RoundingMode.HALF_UP)
                distributedSoy = soyShortfall.multiply(ratio)
                distributedTeriyaki = teriyakiShortfall.multiply(ratio)
                distributedTurmeric = turmericShortfall.multiply(ratio)
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
        val soy: BigDecimal,
        val teriyaki: BigDecimal,
        val turmeric: BigDecimal
    )

    /**
     * Helper data class for projected grams calculations
     */
    private data class ProjectedGrams(
        val soy: Array<BigDecimal>,
        val teriyaki: Array<BigDecimal>,
        val turmeric: Array<BigDecimal>
    )
}