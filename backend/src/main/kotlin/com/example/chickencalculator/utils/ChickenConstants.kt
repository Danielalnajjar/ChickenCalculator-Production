package com.example.chickencalculator.utils

/**
 * Central object for app constants
 */
object ChickenConstants {
    // Portion sizes (grams)
    const val PORTION_SIZE_SOY = 100
    const val PORTION_SIZE_TERIYAKI = 160.0
    const val PORTION_SIZE_TURMERIC = 160.0

    // Grams per cooked pan
    const val GRAMS_PER_PAN_SOY = 3000.0
    const val GRAMS_PER_PAN_TERIYAKI = 3200.0
    const val GRAMS_PER_PAN_TURMERIC = 1500.0

    // Yield factors (cooked yield from raw)
    const val YIELD_FACTOR_SOY = 0.73
    const val YIELD_FACTOR_TERIYAKI = 0.88
    const val YIELD_FACTOR_TURMERIC = 0.86

    // Chicken types
    const val TYPE_SOY = "Soy"
    const val TYPE_TERIYAKI = "Teriyaki"
    const val TYPE_TURMERIC = "Turmeric"

    // Forecast days
    const val FORECAST_DAYS = 4
}