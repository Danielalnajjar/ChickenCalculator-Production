package com.example.chickencalculator.model

import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Current inventory data (morning count of prepared chicken in pans)
 */
data class InventoryData(
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val pansSoy: BigDecimal,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val pansTeriyaki: BigDecimal,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val pansTurmeric: BigDecimal
)

/**
 * Projected sales data for next 4 days (in dollar amounts)
 */
data class ProjectedSales(
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val day0: BigDecimal,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val day1: BigDecimal,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val day2: BigDecimal,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val day3: BigDecimal
) {
    fun getSales(day: Int): BigDecimal {
        return when (day) {
            0 -> day0
            1 -> day1
            2 -> day2
            3 -> day3
            else -> BigDecimal.ZERO
        }
    }
}

/**
 * Result of marination calculation
 */
data class CalculationResult(
    val rawToMarinateSoy: BigDecimal,
    val rawToMarinateTeriyaki: BigDecimal,
    val rawToMarinateTurmeric: BigDecimal,
    val portionsPer1000Soy: BigDecimal,
    val portionsPer1000Teriyaki: BigDecimal,
    val portionsPer1000Turmeric: BigDecimal
)

/**
 * Historical sales totals for calculation
 */
data class SalesTotals(
    val totalSales: BigDecimal,
    val totalPortionsSoy: BigDecimal,
    val totalPortionsTeriyaki: BigDecimal,
    val totalPortionsTurmeric: BigDecimal
) {
    /**
     * Secondary constructor for JPA queries that return Double values.
     * This is needed because JPA/Hibernate native queries with SUM() return Double,
     * but we want to work with BigDecimal for precision in financial calculations.
     */
    constructor(
        totalSales: Double,
        totalPortionsSoy: Double,
        totalPortionsTeriyaki: Double,
        totalPortionsTurmeric: Double
    ) : this(
        BigDecimal.valueOf(totalSales),
        BigDecimal.valueOf(totalPortionsSoy),
        BigDecimal.valueOf(totalPortionsTeriyaki),
        BigDecimal.valueOf(totalPortionsTurmeric)
    )
}

/**
 * Request model for marination calculation
 */
data class MarinationRequest(
    @field:NotNull
    val inventory: InventoryData,
    
    @field:NotNull
    val projectedSales: ProjectedSales,
    
    @field:DecimalMin(value = "0.0", inclusive = true)
    val availableRawChickenKg: BigDecimal? = null,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val alreadyMarinatedSoy: BigDecimal = BigDecimal.ZERO,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val alreadyMarinatedTeriyaki: BigDecimal = BigDecimal.ZERO,
    
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val alreadyMarinatedTurmeric: BigDecimal = BigDecimal.ZERO
)