package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.model.ChickenCalculationRequest
import com.example.chickencalculator.service.ChickenCalculatorService
import com.example.chickencalculator.service.LocationService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.*
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureTestWebMvc
@ActiveProfiles("test")
class ChickenCalculatorControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var chickenCalculatorService: ChickenCalculatorService

    @MockBean
    private lateinit var locationService: LocationService

    private val testLocation = Location(
        id = 1L,
        name = "Test Restaurant",
        slug = "test-restaurant",
        createdAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("Calculate Chicken API Tests")
    inner class CalculateChickenTests {

        @Test
        @DisplayName("Should calculate chicken requirements successfully")
        fun testCalculateChickenSuccess() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = 10,
                piecesPerPerson = 2,
                desiredCookingTime = 45,
                currentTime = "14:00"
            )

            val mockResponse = com.example.chickencalculator.model.ChickenCalculationResponse(
                peopleCount = 10,
                piecesPerPerson = 2,
                totalPieces = 20,
                totalWeight = 3.0,
                marinationTime = 120,
                marinationStartTime = "12:00",
                recommendedCookingTime = 45,
                estimatedFinishTime = "14:45",
                location = "Test Restaurant"
            )

            whenever(locationService.findBySlug("test-restaurant")).thenReturn(testLocation)
            whenever(chickenCalculatorService.calculateChickenRequirements(request, testLocation))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Location-Slug", "test-restaurant")
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.peopleCount").value(10))
                .andExpect(jsonPath("$.piecesPerPerson").value(2))
                .andExpect(jsonPath("$.totalPieces").value(20))
                .andExpect(jsonPath("$.totalWeight").value(3.0))
                .andExpect(jsonPath("$.marinationTime").value(120))
                .andExpect(jsonPath("$.location").value("Test Restaurant"))

            verify(locationService).findBySlug("test-restaurant")
            verify(chickenCalculatorService).calculateChickenRequirements(request, testLocation)
        }

        @Test
        @DisplayName("Should handle calculation without location header")
        fun testCalculateChickenWithoutLocation() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = 5,
                piecesPerPerson = 3,
                desiredCookingTime = 60,
                currentTime = "15:00"
            )

            val mockResponse = com.example.chickencalculator.model.ChickenCalculationResponse(
                peopleCount = 5,
                piecesPerPerson = 3,
                totalPieces = 15,
                totalWeight = 2.25,
                marinationTime = 180,
                marinationStartTime = "12:00",
                recommendedCookingTime = 60,
                estimatedFinishTime = "16:00",
                location = "Default"
            )

            whenever(chickenCalculatorService.calculateChickenRequirements(request, null))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.peopleCount").value(5))
                .andExpect(jsonPath("$.totalPieces").value(15))
                .andExpect(jsonPath("$.location").value("Default"))

            verify(chickenCalculatorService).calculateChickenRequirements(request, null)
        }

        @Test
        @DisplayName("Should return 400 for invalid request body")
        fun testCalculateChickenInvalidRequest() {
            // Given
            val invalidRequest = """{"invalidField": "value"}"""

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("Should return 400 for missing request body")
        fun testCalculateChickenMissingBody() {
            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("Should handle negative values gracefully")
        fun testCalculateChickenNegativeValues() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = -5,
                piecesPerPerson = -2,
                desiredCookingTime = -30,
                currentTime = "14:00"
            )

            val mockResponse = com.example.chickencalculator.model.ChickenCalculationResponse(
                peopleCount = 0,
                piecesPerPerson = 0,
                totalPieces = 0,
                totalWeight = 0.0,
                marinationTime = 0,
                marinationStartTime = "14:00",
                recommendedCookingTime = 30,
                estimatedFinishTime = "14:30",
                location = "Default"
            )

            whenever(chickenCalculatorService.calculateChickenRequirements(request, null))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.peopleCount").value(0))
                .andExpect(jsonPath("$.totalPieces").value(0))
        }
    }

    @Nested
    @DisplayName("Location Resolution Tests")
    inner class LocationResolutionTests {

        @Test
        @DisplayName("Should use location from slug header when provided")
        fun testLocationFromSlugHeader() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = 8,
                piecesPerPerson = 2,
                desiredCookingTime = 40,
                currentTime = "13:00"
            )

            val mockResponse = com.example.chickencalculator.model.ChickenCalculationResponse(
                peopleCount = 8,
                piecesPerPerson = 2,
                totalPieces = 16,
                totalWeight = 2.4,
                marinationTime = 100,
                marinationStartTime = "11:20",
                recommendedCookingTime = 40,
                estimatedFinishTime = "13:40",
                location = "Test Restaurant"
            )

            whenever(locationService.findBySlug("test-restaurant")).thenReturn(testLocation)
            whenever(chickenCalculatorService.calculateChickenRequirements(request, testLocation))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Location-Slug", "test-restaurant")
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)

            verify(locationService).findBySlug("test-restaurant")
            verify(chickenCalculatorService).calculateChickenRequirements(request, testLocation)
        }

        @Test
        @DisplayName("Should handle non-existent location slug gracefully")
        fun testNonExistentLocationSlug() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = 6,
                piecesPerPerson = 2,
                desiredCookingTime = 50,
                currentTime = "12:00"
            )

            val mockResponse = com.example.chickencalculator.model.ChickenCalculationResponse(
                peopleCount = 6,
                piecesPerPerson = 2,
                totalPieces = 12,
                totalWeight = 1.8,
                marinationTime = 120,
                marinationStartTime = "10:00",
                recommendedCookingTime = 50,
                estimatedFinishTime = "12:50",
                location = "Default"
            )

            whenever(locationService.findBySlug("non-existent")).thenReturn(null)
            whenever(chickenCalculatorService.calculateChickenRequirements(request, null))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Location-Slug", "non-existent")
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)

            verify(locationService).findBySlug("non-existent")
            verify(chickenCalculatorService).calculateChickenRequirements(request, null)
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle service exceptions gracefully")
        fun testServiceException() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = 10,
                piecesPerPerson = 2,
                desiredCookingTime = 45,
                currentTime = "14:00"
            )

            whenever(chickenCalculatorService.calculateChickenRequirements(any(), any()))
                .thenThrow(RuntimeException("Service error"))

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isInternalServerError)
        }

        @Test
        @DisplayName("Should handle invalid JSON content type")
        fun testInvalidContentType() {
            // Given
            val request = ChickenCalculationRequest(
                peopleCount = 10,
                piecesPerPerson = 2,
                desiredCookingTime = 45,
                currentTime = "14:00"
            )

            // When & Then
            mockMvc.perform(
                post("/api/calculator/calculate")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnsupportedMediaType)
        }
    }
}