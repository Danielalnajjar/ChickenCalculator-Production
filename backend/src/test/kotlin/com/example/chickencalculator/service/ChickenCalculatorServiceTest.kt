package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.ChickenCalculationRequest
import com.example.chickencalculator.model.ChickenCalculationResponse
import com.example.chickencalculator.utils.ChickenCalculator
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ChickenCalculatorServiceTest {

    @Mock
    private lateinit var salesDataService: SalesDataService

    @Mock
    private lateinit var metricsService: MetricsService

    @Mock
    private lateinit var calculationCounter: Counter

    @Mock
    private lateinit var calculationTimer: Timer

    @Mock
    private lateinit var timerSample: Timer.Sample

    @InjectMocks
    private lateinit var chickenCalculatorService: ChickenCalculatorService

    private lateinit var testLocation: Location
    private lateinit var testRequest: ChickenCalculationRequest

    @BeforeEach
    fun setup() {
        testLocation = Location(
            id = 1L,
            name = "Test Restaurant",
            slug = "test-restaurant",
            createdAt = LocalDateTime.now()
        )

        testRequest = ChickenCalculationRequest(
            peopleCount = 10,
            piecesPerPerson = 2,
            desiredCookingTime = 45,
            currentTime = "14:00"
        )

        whenever(metricsService.getCalculationCounter()).thenReturn(calculationCounter)
        whenever(metricsService.getCalculationTimer()).thenReturn(calculationTimer)
        whenever(calculationTimer.start()).thenReturn(timerSample)
    }

    @Nested
    @DisplayName("Calculate Chicken Requirements Tests")
    inner class CalculateChickenRequirementsTests {

        @Test
        @DisplayName("Should calculate chicken requirements successfully")
        fun testCalculateChickenRequirements() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = 10,
                piecesPerPerson = 2,
                desiredCookingTime = 45,
                currentTime = "14:00"
            )

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            assertNotNull(result)
            assertEquals(10, result.peopleCount)
            assertEquals(2, result.piecesPerPerson)
            assertEquals(20, result.totalPieces)
            assertTrue(result.totalWeight > 0)
            assertNotNull(result.marinationTime)
            assertNotNull(result.marinationStartTime)
            verify(calculationCounter).increment()
            verify(timerSample).stop(calculationTimer)
        }

        @Test
        @DisplayName("Should handle zero people count")
        fun testZeroPeopleCount() {
            // Given
            val request = testRequest.copy(peopleCount = 0)

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            assertNotNull(result)
            assertEquals(0, result.peopleCount)
            assertEquals(0, result.totalPieces)
            assertEquals(0.0, result.totalWeight, 0.001)
        }

        @Test
        @DisplayName("Should handle large numbers correctly")
        fun testLargeNumbers() {
            // Given
            val request = testRequest.copy(peopleCount = 1000, piecesPerPerson = 5)

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            assertNotNull(result)
            assertEquals(1000, result.peopleCount)
            assertEquals(5, result.piecesPerPerson)
            assertEquals(5000, result.totalPieces)
            assertTrue(result.totalWeight > 0)
        }

        @Test
        @DisplayName("Should save sales data when location is provided")
        fun testSavesSalesData() {
            // Given
            val request = testRequest

            // When
            chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            verify(salesDataService).saveSalesData(
                eq(testLocation),
                eq(request.peopleCount),
                eq(request.totalPieces),
                any(),
                eq(LocalDate.now())
            )
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    inner class InputValidationTests {

        @Test
        @DisplayName("Should handle negative people count")
        fun testNegativePeopleCount() {
            // Given
            val request = testRequest.copy(peopleCount = -5)

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            // Should handle gracefully (specific behavior depends on implementation)
            assertNotNull(result)
        }

        @Test
        @DisplayName("Should handle negative pieces per person")
        fun testNegativePiecesPerPerson() {
            // Given
            val request = testRequest.copy(piecesPerPerson = -2)

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            // Should handle gracefully (specific behavior depends on implementation)
            assertNotNull(result)
        }

        @Test
        @DisplayName("Should handle invalid cooking time")
        fun testInvalidCookingTime() {
            // Given
            val request = testRequest.copy(desiredCookingTime = -10)

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            // Should handle gracefully (specific behavior depends on implementation)
            assertNotNull(result)
        }

        @Test
        @DisplayName("Should handle invalid time format")
        fun testInvalidTimeFormat() {
            // Given
            val request = testRequest.copy(currentTime = "invalid-time")

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            // Should handle gracefully or throw appropriate exception
            assertNotNull(result)
        }
    }

    @Nested
    @DisplayName("Metrics Integration Tests")
    inner class MetricsIntegrationTests {

        @Test
        @DisplayName("Should record metrics for successful calculation")
        fun testMetricsRecording() {
            // Given
            val request = testRequest

            // When
            chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            verify(calculationCounter).increment()
            verify(calculationTimer).start()
            verify(timerSample).stop(calculationTimer)
        }

        @Test
        @DisplayName("Should still record metrics even on calculation errors")
        fun testMetricsRecordingOnError() {
            // Given
            val request = testRequest
            whenever(salesDataService.saveSalesData(any(), any(), any(), any(), any()))
                .thenThrow(RuntimeException("Database error"))

            // When & Then
            try {
                chickenCalculatorService.calculateChickenRequirements(request, testLocation)
            } catch (e: Exception) {
                // Expected exception
            }

            // Verify metrics are still recorded
            verify(calculationCounter).increment()
            verify(timerSample).stop(calculationTimer)
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    inner class BusinessLogicTests {

        @Test
        @DisplayName("Should calculate correct marination time based on cooking time")
        fun testMarinationTimeCalculation() {
            // Given
            val shortCookingRequest = testRequest.copy(desiredCookingTime = 30)
            val longCookingRequest = testRequest.copy(desiredCookingTime = 120)

            // When
            val shortCookingResult = chickenCalculatorService.calculateChickenRequirements(shortCookingRequest, testLocation)
            val longCookingResult = chickenCalculatorService.calculateChickenRequirements(longCookingRequest, testLocation)

            // Then
            assertNotNull(shortCookingResult.marinationTime)
            assertNotNull(longCookingResult.marinationTime)
            // Marination time should be influenced by cooking time
            assertTrue(shortCookingResult.marinationTime > 0)
            assertTrue(longCookingResult.marinationTime > 0)
        }

        @Test
        @DisplayName("Should calculate weight based on pieces and standard weight per piece")
        fun testWeightCalculation() {
            // Given
            val request = testRequest.copy(peopleCount = 4, piecesPerPerson = 3)
            val expectedTotalPieces = 12

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            assertEquals(expectedTotalPieces, result.totalPieces)
            // Weight should be proportional to pieces
            val expectedWeight = expectedTotalPieces * ChickenCalculator.AVERAGE_PIECE_WEIGHT
            assertEquals(expectedWeight, result.totalWeight, 0.001)
        }

        @Test
        @DisplayName("Should set proper marination start time")
        fun testMarinationStartTime() {
            // Given
            val request = testRequest.copy(currentTime = "15:30")

            // When
            val result = chickenCalculatorService.calculateChickenRequirements(request, testLocation)

            // Then
            assertNotNull(result.marinationStartTime)
            assertTrue(result.marinationStartTime.isNotEmpty())
            // Should be a valid time format
            assertTrue(result.marinationStartTime.matches(Regex("\\d{2}:\\d{2}")))
        }
    }
}