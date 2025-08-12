package com.example.chickencalculator.controller

import com.example.chickencalculator.exception.LocationNotFoundException
import com.example.chickencalculator.service.LocationService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AdminLocationController::class)
class AdminLocationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var locationService: LocationService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 with correlation ID when location not found`() {
        val locationId = 999L
        whenever(locationService.getLocationByIdOrThrow(locationId))
            .thenThrow(LocationNotFoundException(locationId))

        mockMvc.perform(get("/api/admin/locations/$locationId"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Location Not Found"))
            .andExpect(jsonPath("$.message").value("Location with ID '$locationId' not found"))
            .andExpect(jsonPath("$.path").value("/api/admin/locations/$locationId"))
            .andExpect(jsonPath("$.correlationId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(header().exists("X-Correlation-ID"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 400 with validation errors for invalid location request`() {
        val invalidRequest = CreateLocationRequest(
            name = "", // Invalid: blank name
            address = "123 Test St",
            managerName = "", // Invalid: blank manager name
            managerEmail = "invalid-email" // Invalid: not a valid email
        )

        mockMvc.perform(
            post("/api/admin/locations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.correlationId").exists())
            .andExpect(jsonPath("$.details.fieldErrors").exists())
            .andExpect(header().exists("X-Correlation-ID"))
    }
}