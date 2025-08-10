package com.example.chickencalculator.model

/**
 * Current inventory data (morning count of prepared chicken in pans)
 */
data class InventoryData(
    val pansSoy: Double,
    val pansTeriyaki: Double,
    val pansTurmeric: Double
)

/**
 * Projected sales data for next 4 days (in dollar amounts)
 */
data class ProjectedSales(
    val day0: Double,
    val day1: Double,
    val day2: Double,
    val day3: Double
) {
    fun getSales(day: Int): Double {
        return when (day) {
            0 -> day0
            1 -> day1
            2 -> day2
            3 -> day3
            else -> 0.0
        }
    }
}

/**
 * Result of marination calculation
 */
data class CalculationResult(
    val rawToMarinateSoy: Double,
    val rawToMarinateTeriyaki: Double,
    val rawToMarinateTurmeric: Double,
    val portionsPer1000Soy: Double,
    val portionsPer1000Teriyaki: Double,
    val portionsPer1000Turmeric: Double
)

/**
 * Historical sales totals for calculation
 */
data class SalesTotals(
    val totalSales: Double,
    val totalPortionsSoy: Double,
    val totalPortionsTeriyaki: Double,
    val totalPortionsTurmeric: Double
)

/**
 * Request model for marination calculation
 */
data class MarinationRequest(
    val inventory: InventoryData,
    val projectedSales: ProjectedSales,
    val availableRawChickenKg: Double? = null,
    val alreadyMarinatedSoy: Double = 0.0,
    val alreadyMarinatedTeriyaki: Double = 0.0,
    val alreadyMarinatedTurmeric: Double = 0.0
)