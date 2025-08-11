package com.example.chickencalculator.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.math.BigDecimal
import java.math.RoundingMode

class ChickenCalculatorTest {

    private val calculator = ChickenCalculator()

    @Nested
    @DisplayName("Calculate Raw Chicken Tests")
    inner class CalculateRawChickenTests {
        
        @Test
        @DisplayName("Should calculate correct amount for single day")
        fun testSingleDayCalculation() {
            val result = calculator.calculateRawChicken(
                salesKg = BigDecimal("100"),
                numberOfDays = 1
            )
            
            // 100kg sales * 0.5 conversion factor = 50kg raw chicken
            assertEquals(BigDecimal("50.0"), result)
        }
        
        @Test
        @DisplayName("Should calculate correct amount for multiple days")
        fun testMultipleDayCalculation() {
            val result = calculator.calculateRawChicken(
                salesKg = BigDecimal("100"),
                numberOfDays = 3
            )
            
            // 100kg sales * 3 days * 0.5 conversion = 150kg raw chicken
            assertEquals(BigDecimal("150.0"), result)
        }
        
        @Test
        @DisplayName("Should handle zero sales")
        fun testZeroSales() {
            val result = calculator.calculateRawChicken(
                salesKg = BigDecimal.ZERO,
                numberOfDays = 5
            )
            
            assertEquals(BigDecimal("0.0"), result)
        }
        
        @Test
        @DisplayName("Should handle decimal sales values")
        fun testDecimalSales() {
            val result = calculator.calculateRawChicken(
                salesKg = BigDecimal("75.5"),
                numberOfDays = 2
            )
            
            // 75.5 * 2 * 0.5 = 75.5kg raw chicken
            assertEquals(BigDecimal("75.5"), result)
        }
    }
    
    @Nested
    @DisplayName("Distribute Raw Chicken Tests")
    inner class DistributeRawChickenTests {
        
        @Test
        @DisplayName("Should distribute evenly among flavors")
        fun testEvenDistribution() {
            val inventory = InventoryData(
                currentSoy = BigDecimal("10"),
                currentTeriyaki = BigDecimal("10"),
                currentTurmeric = BigDecimal("10")
            )
            
            val sales = ProjectedSales(
                day1 = DailySales(
                    totalSales = BigDecimal("90"),
                    portionsSoy = BigDecimal("30"),
                    portionsTeriyaki = BigDecimal("30"),
                    portionsTurmeric = BigDecimal("30")
                )
            )
            
            val result = calculator.distributeRawChicken(
                rawChickenKg = BigDecimal("90"),
                inventory = inventory,
                projectedSales = sales
            )
            
            assertNotNull(result)
            // Each flavor should get approximately 30kg
            assertTrue(result.soy > BigDecimal.ZERO)
            assertTrue(result.teriyaki > BigDecimal.ZERO)
            assertTrue(result.turmeric > BigDecimal.ZERO)
            
            // Total should equal input
            val total = result.soy + result.teriyaki + result.turmeric
            assertEquals(0, total.compareTo(BigDecimal("90")))
        }
        
        @Test
        @DisplayName("Should handle zero raw chicken")
        fun testZeroRawChicken() {
            val inventory = InventoryData(
                currentSoy = BigDecimal("10"),
                currentTeriyaki = BigDecimal("10"),
                currentTurmeric = BigDecimal("10")
            )
            
            val sales = ProjectedSales(
                day1 = DailySales(
                    totalSales = BigDecimal("90"),
                    portionsSoy = BigDecimal("30"),
                    portionsTeriyaki = BigDecimal("30"),
                    portionsTurmeric = BigDecimal("30")
                )
            )
            
            val result = calculator.distributeRawChicken(
                rawChickenKg = BigDecimal.ZERO,
                inventory = inventory,
                projectedSales = sales
            )
            
            assertEquals(BigDecimal.ZERO, result.soy)
            assertEquals(BigDecimal.ZERO, result.teriyaki)
            assertEquals(BigDecimal.ZERO, result.turmeric)
        }
        
        @Test
        @DisplayName("Should prioritize flavors with lower inventory")
        fun testInventoryPrioritization() {
            val inventory = InventoryData(
                currentSoy = BigDecimal("5"),  // Low inventory
                currentTeriyaki = BigDecimal("20"), // High inventory
                currentTurmeric = BigDecimal("15")  // Medium inventory
            )
            
            val sales = ProjectedSales(
                day1 = DailySales(
                    totalSales = BigDecimal("90"),
                    portionsSoy = BigDecimal("30"),
                    portionsTeriyaki = BigDecimal("30"),
                    portionsTurmeric = BigDecimal("30")
                )
            )
            
            val result = calculator.distributeRawChicken(
                rawChickenKg = BigDecimal("60"),
                inventory = inventory,
                projectedSales = sales
            )
            
            // Soy should get more allocation due to lower inventory
            assertTrue(result.soy > result.teriyaki)
        }
    }
    
    @Nested
    @DisplayName("Calculate Totals Tests")
    inner class CalculateTotalsTests {
        
        @Test
        @DisplayName("Should calculate totals for single day")
        fun testSingleDayTotals() {
            val sales = ProjectedSales(
                day1 = DailySales(
                    totalSales = BigDecimal("100"),
                    portionsSoy = BigDecimal("40"),
                    portionsTeriyaki = BigDecimal("35"),
                    portionsTurmeric = BigDecimal("25")
                )
            )
            
            val totals = calculator.calculateTotals(sales)
            
            assertEquals(BigDecimal("100"), totals.totalSales)
            assertEquals(BigDecimal("40"), totals.totalPortionsSoy)
            assertEquals(BigDecimal("35"), totals.totalPortionsTeriyaki)
            assertEquals(BigDecimal("25"), totals.totalPortionsTurmeric)
        }
        
        @Test
        @DisplayName("Should calculate totals for multiple days")
        fun testMultipleDayTotals() {
            val sales = ProjectedSales(
                day1 = DailySales(
                    totalSales = BigDecimal("100"),
                    portionsSoy = BigDecimal("40"),
                    portionsTeriyaki = BigDecimal("35"),
                    portionsTurmeric = BigDecimal("25")
                ),
                day2 = DailySales(
                    totalSales = BigDecimal("120"),
                    portionsSoy = BigDecimal("50"),
                    portionsTeriyaki = BigDecimal("40"),
                    portionsTurmeric = BigDecimal("30")
                ),
                day3 = DailySales(
                    totalSales = BigDecimal("80"),
                    portionsSoy = BigDecimal("30"),
                    portionsTeriyaki = BigDecimal("25"),
                    portionsTurmeric = BigDecimal("25")
                )
            )
            
            val totals = calculator.calculateTotals(sales)
            
            assertEquals(BigDecimal("300"), totals.totalSales)
            assertEquals(BigDecimal("120"), totals.totalPortionsSoy)
            assertEquals(BigDecimal("100"), totals.totalPortionsTeriyaki)
            assertEquals(BigDecimal("80"), totals.totalPortionsTurmeric)
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle negative days gracefully")
        fun testNegativeDays() {
            val result = calculator.calculateRawChicken(
                salesKg = BigDecimal("100"),
                numberOfDays = -1
            )
            
            // Should treat negative as zero or throw exception
            assertTrue(result >= BigDecimal.ZERO)
        }
        
        @Test
        @DisplayName("Should handle very large numbers")
        fun testLargeNumbers() {
            val result = calculator.calculateRawChicken(
                salesKg = BigDecimal("999999"),
                numberOfDays = 365
            )
            
            assertNotNull(result)
            assertTrue(result > BigDecimal.ZERO)
        }
        
        @Test
        @DisplayName("Should maintain precision with decimal calculations")
        fun testPrecision() {
            val result = calculator.calculateRawChicken(
                salesKg = BigDecimal("33.33"),
                numberOfDays = 3
            )
            
            // 33.33 * 3 * 0.5 = 49.995, should round to 50.0
            val expected = BigDecimal("50.0")
            assertEquals(0, expected.compareTo(result))
        }
    }
}