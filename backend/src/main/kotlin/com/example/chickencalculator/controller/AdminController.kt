package com.example.chickencalculator.controller

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.service.AdminService
import com.example.chickencalculator.service.LocationService
import com.example.chickencalculator.service.JwtService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val id: String, val email: String, val name: String, val role: String, val token: String?)

data class CreateLocationRequest(
    val name: String,
    val domain: String,
    val address: String?,
    val managerName: String,
    val managerEmail: String,
    val cloudProvider: String,
    val region: String
)

data class CreateLocationResponse(
    val id: Long,
    val message: String,
    val status: String,
    val domain: String
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
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
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
            deployingLocations = locations.count { it.status == LocationStatus.DEPLOYING },
            errorLocations = locations.count { it.status == LocationStatus.ERROR }
        )
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/locations")
    fun getAllLocations(): ResponseEntity<List<Location>> {
        val locations = locationService.getAllLocations()
        return ResponseEntity.ok(locations)
    }
    
    @PostMapping("/locations")
    fun createLocation(@RequestBody request: CreateLocationRequest): ResponseEntity<CreateLocationResponse> {
        try {
            val location = locationService.createLocation(
                name = request.name,
                domain = if (request.domain.endsWith(".yourcompany.com")) {
                    request.domain
                } else {
                    "${request.domain}.yourcompany.com"
                },
                address = request.address,
                managerName = request.managerName,
                managerEmail = request.managerEmail,
                cloudProvider = request.cloudProvider,
                region = request.region
            )
            
            // Start deployment process asynchronously
            locationService.deployLocation(location)
            
            return ResponseEntity.ok(CreateLocationResponse(
                id = location.id,
                message = "Location deployment started",
                status = "deploying",
                domain = location.domain
            ))
        } catch (e: Exception) {
            return ResponseEntity.status(500).body(CreateLocationResponse(
                id = 0,
                message = "Failed to create location: ${e.message}",
                status = "error",
                domain = ""
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