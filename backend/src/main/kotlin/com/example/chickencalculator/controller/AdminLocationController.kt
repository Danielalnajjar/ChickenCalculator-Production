package com.example.chickencalculator.controller

import com.example.chickencalculator.config.ApiVersionConfig
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.service.LocationManagementService
import com.example.chickencalculator.service.LocationAuthService
import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
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

data class UpdateLocationPasswordRequest(
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)

@RestController
@RequestMapping("${ApiVersionConfig.API_VERSION}/admin")
@Tag(name = "Admin Location Management", description = "Admin location management endpoints")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS]
)
class AdminLocationController(
    private val locationManagementService: LocationManagementService,
    private val locationAuthService: LocationAuthService,
    private val metricsService: MetricsService
) {
    private val logger = LoggerFactory.getLogger(AdminLocationController::class.java)
    
    @GetMapping("/stats")
    @Timed(value = "chicken.calculator.admin.stats.time", description = "Time taken to get dashboard statistics")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve dashboard statistics including location counts and status")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    ])
    fun getDashboardStats(): ResponseEntity<DashboardStats> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val locations = locationManagementService.getAllLocations()
            val stats = DashboardStats(
                totalLocations = locations.size,
                activeLocations = locations.count { it.status == LocationStatus.ACTIVE },
                deployingLocations = 0, // No longer deploying locations
                errorLocations = locations.count { it.status == LocationStatus.INACTIVE }
            )
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordAdminOperation("get_stats", true, processingTime)
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordAdminOperation("get_stats", false, processingTime)
            metricsService.recordError("admin_get_stats", e.javaClass.simpleName)
            throw e
        }
    }
    
    @GetMapping("/locations")
    @Timed(value = "chicken.calculator.admin.get_locations.time", description = "Time taken to get all locations")
    @Operation(summary = "Get all locations", description = "Retrieve a list of all locations")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Locations retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    ])
    fun getAllLocations(): ResponseEntity<List<Location>> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val locations = locationManagementService.getAllLocations()
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordAdminOperation("get_all_locations", true, processingTime)
            ResponseEntity.ok(locations)
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordAdminOperation("get_all_locations", false, processingTime)
            metricsService.recordError("admin_get_locations", e.javaClass.simpleName)
            throw e
        }
    }
    
    @PostMapping("/locations")
    @Timed(value = "chicken.calculator.admin.create_location.time", description = "Time taken to create location")
    @Operation(summary = "Create new location", description = "Create a new location with manager details")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Location created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    fun createLocation(@Valid @RequestBody request: CreateLocationRequest): ResponseEntity<CreateLocationResponse> {
        val startTime = System.currentTimeMillis()
        logger.info("Received location creation request for: ${request.name}")
        logger.debug("Request details - Name: ${request.name}, Manager: ${request.managerName}, Email: ${request.managerEmail}")
        
        return try {
            // LocationManagementService.createLocation now throws appropriate exceptions
            val location = locationManagementService.createLocation(
                name = request.name,
                address = request.address,
                managerName = request.managerName,
                managerEmail = request.managerEmail
            )
            val processingTime = System.currentTimeMillis() - startTime
            
            logger.info("✅ Location created successfully: ${location.name} with slug: ${location.slug}")
            
            metricsService.recordLocationCreated(location.slug)
            metricsService.recordAdminOperation("create_location", true, processingTime)
            
            ResponseEntity.ok(CreateLocationResponse(
                id = location.id,
                message = "Location created successfully",
                status = "active",
                slug = location.slug
            ))
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordAdminOperation("create_location", false, processingTime)
            metricsService.recordError("admin_create_location", e.javaClass.simpleName)
            throw e
        }
    }
    
    @DeleteMapping("/locations/{id}")
    @Timed(value = "chicken.calculator.admin.delete_location.time", description = "Time taken to delete location")
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
        val startTime = System.currentTimeMillis()
        
        return try {
            // Get location info before deletion for metrics
            val location = locationManagementService.getLocationByIdOrThrow(id)
            val locationSlug = location.slug
            
            // LocationManagementService.deleteLocation now throws appropriate exceptions
            val result = locationManagementService.deleteLocation(id)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordLocationDeleted(locationSlug)
            metricsService.recordAdminOperation("delete_location", true, processingTime)
            
            ResponseEntity.ok(mapOf(
                "message" to result.message,
                "softDelete" to result.softDelete.toString()
            ))
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordAdminOperation("delete_location", false, processingTime)
            metricsService.recordError("admin_delete_location", e.javaClass.simpleName)
            throw e
        }
    }
    
    @GetMapping("/locations/{id}")
    @Timed(value = "chicken.calculator.admin.get_location.time", description = "Time taken to get location by ID")
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
        val startTime = System.currentTimeMillis()
        
        return try {
            // Use the new method that throws appropriate exceptions
            val location = locationManagementService.getLocationByIdOrThrow(id)
            val processingTime = System.currentTimeMillis() - startTime
            
            metricsService.recordAdminOperation("get_location", true, processingTime)
            ResponseEntity.ok(location)
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordAdminOperation("get_location", false, processingTime)
            metricsService.recordError("admin_get_location", e.javaClass.simpleName)
            throw e
        }
    }
    
    @PutMapping("/locations/{id}/password")
    @Timed(value = "chicken.calculator.admin.update_password.time", description = "Time taken to update location password")
    @Operation(summary = "Update location password", description = "Set or update the password for a location")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password updated successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "Location not found")
    ])
    fun updateLocationPassword(
        @Parameter(description = "ID of the location")
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLocationPasswordRequest
    ): ResponseEntity<Map<String, String>> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val location = locationAuthService.changeLocationPassword(id, request.password)
            val processingTime = System.currentTimeMillis() - startTime
            
            logger.info("Password updated for location: ${location.slug}")
            metricsService.recordAdminOperation("update_location_password", true, processingTime)
            
            ResponseEntity.ok(mapOf(
                "message" to "Password updated successfully",
                "locationId" to location.id.toString(),
                "locationSlug" to location.slug
            ))
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordAdminOperation("update_location_password", false, processingTime)
            metricsService.recordError("admin_update_password", e.javaClass.simpleName)
            throw e
        }
    }
    
    @PostMapping("/locations/{id}/generate-password")
    @Timed(value = "chicken.calculator.admin.generate_password.time", description = "Time taken to generate location password")
    @Operation(summary = "Generate random password", description = "Generate a secure random password for a location")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password generated successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "Location not found")
    ])
    fun generateLocationPassword(
        @Parameter(description = "ID of the location")
        @PathVariable id: Long
    ): ResponseEntity<Map<String, String>> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val newPassword = locationAuthService.generateSecurePassword()
            val location = locationAuthService.changeLocationPassword(id, newPassword)
            val processingTime = System.currentTimeMillis() - startTime
            
            logger.info("Generated new password for location: ${location.slug}")
            metricsService.recordAdminOperation("generate_location_password", true, processingTime)
            
            ResponseEntity.ok(mapOf(
                "message" to "Password generated successfully",
                "password" to newPassword,
                "locationId" to location.id.toString(),
                "locationSlug" to location.slug,
                "warning" to "Please save this password securely. It will not be shown again."
            ))
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            metricsService.recordAdminOperation("generate_location_password", false, processingTime)
            metricsService.recordError("admin_generate_password", e.javaClass.simpleName)
            throw e
        }
    }
}