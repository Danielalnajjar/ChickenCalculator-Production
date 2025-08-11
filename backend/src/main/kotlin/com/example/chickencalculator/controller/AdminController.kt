package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.service.AdminService
import com.example.chickencalculator.service.LocationService
import com.example.chickencalculator.service.JwtService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
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

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String
)

data class LoginResponse(val id: String, val email: String, val name: String, val role: String, val token: String?)

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
@Tag(name = "Admin", description = "Admin authentication and management endpoints")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS]
)
class AdminController(
    private val adminService: AdminService,
    private val locationService: LocationService,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(AdminController::class.java)
    
    @PostMapping("/auth/login")
    @Operation(summary = "Admin login", description = "Authenticate admin user and receive JWT token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ApiResponse(responseCode = "400", description = "Invalid request format")
    ])
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        // Proper authentication using AdminService
        val adminUser = adminService.authenticate(request.email, request.password)
        
        return if (adminUser != null) {
            // Generate JWT token
            val token = jwtService.generateToken(
                email = adminUser.email,
                userId = adminUser.id!!,
                role = adminUser.role.name
            )
            
            ResponseEntity.ok(LoginResponse(
                id = adminUser.id.toString(),
                email = adminUser.email,
                name = adminUser.name,
                role = adminUser.role.name.lowercase(),
                token = token
            ))
        } else {
            ResponseEntity.status(401).build()
        }
    }
    
    @PostMapping("/auth/validate")
    fun validateToken(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<LoginResponse> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build()
        }
        
        val token = authHeader.substring(7)
        
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(401).build()
        }
        
        val email = jwtService.getEmailFromToken(token)
        val userId = jwtService.getUserIdFromToken(token)
        val role = jwtService.getRoleFromToken(token)
        
        if (email == null || userId == null || role == null) {
            return ResponseEntity.status(401).build()
        }
        
        // Get fresh user data from database
        val adminUser = adminService.getAdminByEmail(email)
        
        return if (adminUser != null) {
            ResponseEntity.ok(LoginResponse(
                id = adminUser.id.toString(),
                email = adminUser.email,
                name = adminUser.name,
                role = adminUser.role.name.lowercase(),
                token = null // Don't regenerate token on validation
            ))
        } else {
            ResponseEntity.status(401).build()
        }
    }
    
    // OPTIONS handler for CORS preflight
    @RequestMapping("/auth/login", method = [RequestMethod.OPTIONS])
    fun loginOptions(): ResponseEntity<Void> {
        logger.debug("OPTIONS request for /auth/login")
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "POST, OPTIONS")
            .header("Access-Control-Allow-Headers", "Content-Type")
            .build()
    }
    
    @GetMapping("/stats")
    fun getDashboardStats(): ResponseEntity<DashboardStats> {
        val locations = locationService.getAllLocations()
        val stats = DashboardStats(
            totalLocations = locations.size,
            activeLocations = locations.count { it.status == LocationStatus.ACTIVE },
            deployingLocations = 0, // No longer deploying locations
            errorLocations = locations.count { it.status == LocationStatus.INACTIVE }
        )
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/locations")
    fun getAllLocations(): ResponseEntity<List<Location>> {
        val locations = locationService.getAllLocations()
        return ResponseEntity.ok(locations)
    }
    
    @PostMapping("/locations")
    fun createLocation(@Valid @RequestBody request: CreateLocationRequest): ResponseEntity<CreateLocationResponse> {
        try {
            val location = locationService.createLocation(
                name = request.name,
                address = request.address,
                managerName = request.managerName,
                managerEmail = request.managerEmail
            )
            
            logger.info("Location created: ${location.name} with slug: ${location.slug}")
            
            return ResponseEntity.ok(CreateLocationResponse(
                id = location.id,
                message = "Location created successfully",
                status = "active",
                slug = location.slug
            ))
        } catch (e: Exception) {
            logger.error("Failed to create location: ${e.message}", e)
            return ResponseEntity.status(500).body(CreateLocationResponse(
                id = 0,
                message = "Failed to create location: ${e.message}",
                status = "error",
                slug = ""
            ))
        }
    }
    
    @DeleteMapping("/locations/{id}")
    fun deleteLocation(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        return try {
            locationService.deleteLocation(id)
            ResponseEntity.ok(mapOf("message" to "Location deleted successfully"))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("error" to e.message.orEmpty()))
        }
    }
    
    @GetMapping("/locations/{id}")
    fun getLocation(@PathVariable id: Long): ResponseEntity<Location> {
        val location = locationService.getLocationById(id)
        return if (location != null) {
            ResponseEntity.ok(location)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}