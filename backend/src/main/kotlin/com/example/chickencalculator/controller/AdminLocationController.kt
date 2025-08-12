package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.service.LocationManagementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class CreateLocationRequest(
    @field:NotBlank(message = "Location name is required")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val name: String,
    
    val address: String?,
    
    @field:NotBlank(message = "Manager name is required")
    @field:Size(min = 2, max = 100, message = "Manager name must be between 2 and 100 characters")
    val managerName: String,
    
    @field:NotBlank(message = "Manager email is required")
    @field:Email(message = "Invalid manager email format")
    val managerEmail: String
)

data class CreateLocationResponse(
    val id: Long,
    val message: String,
    val status: String,
    val slug: String
)

data class DashboardStats(
    val totalLocations: Int,
    val activeLocations: Int,
    val deployingLocations: Int,
    val errorLocations: Int,
    val totalTransactions: Long = 0,
    val totalRevenue: Double = 0.0
)

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Location Management", description = "Admin location management endpoints")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS]
)
class AdminLocationController(
    private val locationManagementService: LocationManagementService
) {
    private val logger = LoggerFactory.getLogger(AdminLocationController::class.java)
    
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve dashboard statistics including location counts and status")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    ])
    fun getDashboardStats(): ResponseEntity<DashboardStats> {
        val locations = locationManagementService.getAllLocations()
        val stats = DashboardStats(
            totalLocations = locations.size,
            activeLocations = locations.count { it.status == LocationStatus.ACTIVE },
            deployingLocations = 0, // No longer deploying locations
            errorLocations = locations.count { it.status == LocationStatus.INACTIVE }
        )
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/locations")
    @Operation(summary = "Get all locations", description = "Retrieve a list of all locations")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Locations retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    ])
    fun getAllLocations(): ResponseEntity<List<Location>> {
        val locations = locationManagementService.getAllLocations()
        return ResponseEntity.ok(locations)
    }
    
    @PostMapping("/locations")
    @Operation(summary = "Create new location", description = "Create a new location with manager details")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Location created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun createLocation(@Valid @RequestBody request: CreateLocationRequest): ResponseEntity<CreateLocationResponse> {
        logger.info("Received location creation request for: ${request.name}")
        logger.debug("Request details - Name: ${request.name}, Manager: ${request.managerName}, Email: ${request.managerEmail}")
        
        // LocationManagementService.createLocation now throws appropriate exceptions
        val location = locationManagementService.createLocation(
            name = request.name,
            address = request.address,
            managerName = request.managerName,
            managerEmail = request.managerEmail
        )
        
        logger.info("✅ Location created successfully: ${location.name} with slug: ${location.slug}")
        
        return ResponseEntity.ok(CreateLocationResponse(
            id = location.id,
            message = "Location created successfully",
            status = "active",
            slug = location.slug
        ))
    }
    
    @DeleteMapping("/locations/{id}")
    @Operation(summary = "Delete location", description = "Delete a location by its ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Location deleted successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "Location not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun deleteLocation(
        @Parameter(description = "ID of the location to delete")
        @PathVariable id: Long
    ): ResponseEntity<Map<String, String>> {
        // LocationManagementService.deleteLocation now throws appropriate exceptions
        val result = locationManagementService.deleteLocation(id)
        return ResponseEntity.ok(mapOf(
            "message" to result.message,
            "softDelete" to result.softDelete.toString()
        ))
    }
    
    @GetMapping("/locations/{id}")
    @Operation(summary = "Get location by ID", description = "Retrieve a specific location by its ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Location retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "Location not found")
    ])
    fun getLocation(
        @Parameter(description = "ID of the location to retrieve")
        @PathVariable id: Long
    ): ResponseEntity<Location> {
        // Use the new method that throws appropriate exceptions
        val location = locationManagementService.getLocationByIdOrThrow(id)
        return ResponseEntity.ok(location)
    }
}