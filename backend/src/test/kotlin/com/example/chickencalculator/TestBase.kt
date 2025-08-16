package com.example.chickencalculator

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.entity.MarinationLog
import com.example.chickencalculator.model.MarinationRequest
import com.example.chickencalculator.model.InventoryData
import com.example.chickencalculator.model.ProjectedSales
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Base class for tests providing common test data and utilities.
 */
abstract class TestBase {

    object TestConstants {
        const val DEFAULT_ADMIN_EMAIL = "admin@test.com"
        const val DEFAULT_ADMIN_PASSWORD = "TestPassword123!"
        const val DEFAULT_ADMIN_NAME = "Test Admin"
    }

    companion object {
        // Test Admin Users
        fun createTestAdmin(
            id: Long = 1L,
            email: String = "admin@test.com",
            name: String = "Test Admin",
            role: AdminRole = AdminRole.ADMIN
        ): AdminUser {
            return AdminUser(
                id = id,
                email = email,
                passwordHash = "\$2a\$10\$hashedpassword123",
                name = name,
                role = role,
                createdAt = LocalDateTime.now(),
                lastLoginAt = null
            )
        }

        // Test Locations
        fun createTestLocation(
            id: Long = 1L,
            name: String = "Test Restaurant",
            slug: String = "test-restaurant",
            managerName: String = "Test Manager",
            managerEmail: String = "manager@test.com",
            address: String? = "123 Test Street"
        ): Location {
            return Location(
                id = id,
                name = name,
                slug = slug,
                managerName = managerName,
                managerEmail = managerEmail,
                address = address,
                createdAt = LocalDateTime.now()
            )
        }

        // Test Sales Data
        fun createTestSalesData(
            id: Long = 1L,
            location: Location = createTestLocation(),
            totalSales: BigDecimal = BigDecimal("100.00"),
            portionsSoy: BigDecimal = BigDecimal("30.00"),
            portionsTeriyaki: BigDecimal = BigDecimal("40.00"),
            portionsTurmeric: BigDecimal = BigDecimal("30.00"),
            date: LocalDate = LocalDate.now()
        ): SalesData {
            return SalesData(
                id = id,
                location = location,
                totalSales = totalSales,
                portionsSoy = portionsSoy,
                portionsTeriyaki = portionsTeriyaki,
                portionsTurmeric = portionsTurmeric,
                date = date
            )
        }

        // Test Marination Log
        fun createTestMarinationLog(
            id: Long = 1L,
            location: Location = createTestLocation(),
            soySuggested: BigDecimal = BigDecimal("10.0"),
            teriyakiSuggested: BigDecimal = BigDecimal("10.0"),
            turmericSuggested: BigDecimal = BigDecimal("10.0"),
            soyPans: BigDecimal = BigDecimal("2.0"),
            teriyakiPans: BigDecimal = BigDecimal("2.0"),
            turmericPans: BigDecimal = BigDecimal("2.0"),
            isEndOfDay: Boolean = false
        ): MarinationLog {
            return MarinationLog(
                id = id,
                location = location,
                soySuggested = soySuggested,
                teriyakiSuggested = teriyakiSuggested,
                turmericSuggested = turmericSuggested,
                soyPans = soyPans,
                teriyakiPans = teriyakiPans,
                turmericPans = turmericPans,
                isEndOfDay = isEndOfDay,
                timestamp = LocalDateTime.now()
            )
        }

        // Test Marination Requests
        fun createTestMarinationRequest(
            pansSoy: BigDecimal = BigDecimal("10.0"),
            pansTeriyaki: BigDecimal = BigDecimal("10.0"),
            pansTurmeric: BigDecimal = BigDecimal("10.0"),
            salesDay0: BigDecimal = BigDecimal("50.0"),
            salesDay1: BigDecimal = BigDecimal("60.0"),
            salesDay2: BigDecimal = BigDecimal("55.0"),
            salesDay3: BigDecimal = BigDecimal("45.0"),
            availableRawChickenKg: BigDecimal? = BigDecimal("10.0"),
            alreadyMarinatedSoy: BigDecimal = BigDecimal("5.0"),
            alreadyMarinatedTeriyaki: BigDecimal = BigDecimal("5.0"),
            alreadyMarinatedTurmeric: BigDecimal = BigDecimal("5.0")
        ): MarinationRequest {
            return MarinationRequest(
                inventory = InventoryData(
                    pansSoy = pansSoy,
                    pansTeriyaki = pansTeriyaki,
                    pansTurmeric = pansTurmeric
                ),
                projectedSales = ProjectedSales(
                    day0 = salesDay0,
                    day1 = salesDay1,
                    day2 = salesDay2,
                    day3 = salesDay3
                ),
                availableRawChickenKg = availableRawChickenKg,
                alreadyMarinatedSoy = alreadyMarinatedSoy,
                alreadyMarinatedTeriyaki = alreadyMarinatedTeriyaki,
                alreadyMarinatedTurmeric = alreadyMarinatedTurmeric
            )
        }

        // Multiple Test Locations
        fun createMultipleTestLocations(): List<Location> {
            return listOf(
                createTestLocation(1L, "Main Restaurant", "main-restaurant"),
                createTestLocation(2L, "Downtown Branch", "downtown-branch"),
                createTestLocation(3L, "Airport Location", "airport-location"),
                createTestLocation(4L, "Mall Food Court", "mall-food-court")
            )
        }

        // Multiple Test Sales Data
        fun createMultipleTestSalesData(location: Location): List<SalesData> {
            return listOf(
                createTestSalesData(1L, location, BigDecimal("100.00"), BigDecimal("30.00"), BigDecimal("40.00"), BigDecimal("30.00"), LocalDate.now()),
                createTestSalesData(2L, location, BigDecimal("150.00"), BigDecimal("45.00"), BigDecimal("60.00"), BigDecimal("45.00"), LocalDate.now().minusDays(1)),
                createTestSalesData(3L, location, BigDecimal("80.00"), BigDecimal("24.00"), BigDecimal("32.00"), BigDecimal("24.00"), LocalDate.now().minusDays(2)),
                createTestSalesData(4L, location, BigDecimal("250.00"), BigDecimal("75.00"), BigDecimal("100.00"), BigDecimal("75.00"), LocalDate.now().minusWeeks(1))
            )
        }

        // Test Data Validation Helpers
        fun createInvalidMarinationRequests(): List<MarinationRequest> {
            return listOf(
                // Negative values
                createTestMarinationRequest(pansSoy = BigDecimal("-5.0")),
                createTestMarinationRequest(salesDay0 = BigDecimal("-10.0")),
                createTestMarinationRequest(availableRawChickenKg = BigDecimal("-5.0")),
                // Zero values
                createTestMarinationRequest(pansSoy = BigDecimal.ZERO),
                createTestMarinationRequest(salesDay0 = BigDecimal.ZERO),
                // Very large values
                createTestMarinationRequest(pansSoy = BigDecimal("10000.0")),
                createTestMarinationRequest(salesDay0 = BigDecimal("10000.0")),
                // Null chicken weight
                createTestMarinationRequest(availableRawChickenKg = null)
            )
        }

        // Edge Case Test Data
        fun createEdgeCaseMarinationRequests(): List<MarinationRequest> {
            return listOf(
                // Minimum valid values
                createTestMarinationRequest(pansSoy = BigDecimal("0.1"), salesDay0 = BigDecimal("1.0")),
                // Maximum reasonable values
                createTestMarinationRequest(pansSoy = BigDecimal("100.0"), salesDay0 = BigDecimal("500.0")),
                // Various chicken weights
                createTestMarinationRequest(availableRawChickenKg = BigDecimal("0.5")),
                createTestMarinationRequest(availableRawChickenKg = BigDecimal("100.0")),
                createTestMarinationRequest(availableRawChickenKg = BigDecimal("50.25"))
            )
        }

        // Test JWT Token Helper
        fun createTestJwtToken(): String {
            return "test.jwt.token"
        }

        // Test Error Messages
        object TestErrorMessages {
            const val LOCATION_NOT_FOUND = "Location not found"
            const val ADMIN_NOT_FOUND = "Admin user not found"
            const val INVALID_CREDENTIALS = "Invalid credentials"
            const val UNAUTHORIZED = "Unauthorized access"
            const val BAD_REQUEST = "Bad request"
            const val INTERNAL_ERROR = "Internal server error"
        }

        // Test Constants
        object TestConstants {
            const val DEFAULT_PASSWORD = "TestPassword123!"
            const val DEFAULT_ADMIN_EMAIL = "admin@test.com"
            const val DEFAULT_ADMIN_NAME = "Test Admin"
            const val DEFAULT_LOCATION_NAME = "Test Restaurant"
            const val DEFAULT_LOCATION_SLUG = "test-restaurant"
            const val DEFAULT_PIECES_PER_PERSON = 2
            const val DEFAULT_PEOPLE_COUNT = 10
            const val DEFAULT_COOKING_TIME = 45
            const val DEFAULT_CURRENT_TIME = "14:00"
        }
    }
}