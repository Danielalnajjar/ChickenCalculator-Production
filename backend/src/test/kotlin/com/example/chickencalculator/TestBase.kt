package com.example.chickencalculator

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.entity.MarinationLog
import com.example.chickencalculator.model.ChickenCalculationRequest
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Base class for tests providing common test data and utilities.
 */
abstract class TestBase {

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
            slug: String = "test-restaurant"
        ): Location {
            return Location(
                id = id,
                name = name,
                slug = slug,
                createdAt = LocalDateTime.now()
            )
        }

        // Test Sales Data
        fun createTestSalesData(
            id: Long = 1L,
            location: Location = createTestLocation(),
            peopleCount: Int = 10,
            totalPieces: Int = 20,
            totalWeight: Double = 3.0,
            date: LocalDate = LocalDate.now()
        ): SalesData {
            return SalesData(
                id = id,
                location = location,
                peopleCount = peopleCount,
                totalPieces = totalPieces,
                totalWeight = totalWeight,
                timestamp = LocalDateTime.now(),
                date = date
            )
        }

        // Test Marination Log
        fun createTestMarinationLog(
            id: Long = 1L,
            location: Location = createTestLocation(),
            pieces: Int = 20,
            weight: Double = 3.0,
            marinationTime: Int = 120,
            date: LocalDate = LocalDate.now()
        ): MarinationLog {
            return MarinationLog(
                id = id,
                location = location,
                pieces = pieces,
                weight = weight,
                marinationTime = marinationTime,
                timestamp = LocalDateTime.now(),
                date = date
            )
        }

        // Test Chicken Calculation Requests
        fun createTestCalculationRequest(
            peopleCount: Int = 10,
            piecesPerPerson: Int = 2,
            desiredCookingTime: Int = 45,
            currentTime: String = "14:00"
        ): ChickenCalculationRequest {
            return ChickenCalculationRequest(
                peopleCount = peopleCount,
                piecesPerPerson = piecesPerPerson,
                desiredCookingTime = desiredCookingTime,
                currentTime = currentTime
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
                createTestSalesData(1L, location, 10, 20, 3.0, LocalDate.now()),
                createTestSalesData(2L, location, 15, 30, 4.5, LocalDate.now().minusDays(1)),
                createTestSalesData(3L, location, 8, 16, 2.4, LocalDate.now().minusDays(2)),
                createTestSalesData(4L, location, 25, 50, 7.5, LocalDate.now().minusWeeks(1))
            )
        }

        // Test Data Validation Helpers
        fun createInvalidCalculationRequests(): List<ChickenCalculationRequest> {
            return listOf(
                // Negative values
                createTestCalculationRequest(peopleCount = -5),
                createTestCalculationRequest(piecesPerPerson = -2),
                createTestCalculationRequest(desiredCookingTime = -30),
                // Zero values
                createTestCalculationRequest(peopleCount = 0),
                createTestCalculationRequest(piecesPerPerson = 0),
                // Very large values
                createTestCalculationRequest(peopleCount = 10000),
                createTestCalculationRequest(piecesPerPerson = 100),
                // Invalid time format
                createTestCalculationRequest(currentTime = "invalid-time"),
                createTestCalculationRequest(currentTime = "25:00"),
                createTestCalculationRequest(currentTime = "12:70")
            )
        }

        // Edge Case Test Data
        fun createEdgeCaseCalculationRequests(): List<ChickenCalculationRequest> {
            return listOf(
                // Minimum valid values
                createTestCalculationRequest(peopleCount = 1, piecesPerPerson = 1, desiredCookingTime = 1),
                // Maximum reasonable values
                createTestCalculationRequest(peopleCount = 500, piecesPerPerson = 10, desiredCookingTime = 480),
                // Various time formats
                createTestCalculationRequest(currentTime = "00:00"),
                createTestCalculationRequest(currentTime = "23:59"),
                createTestCalculationRequest(currentTime = "12:30")
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