package com.example.chickencalculator.controller

import com.example.chickencalculator.config.ApiVersionConfig
import com.example.chickencalculator.dto.PasswordChangeRequest
import com.example.chickencalculator.service.AdminService
import com.example.chickencalculator.service.JwtService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.*

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String
)

data class LoginResponse(
    val id: String, 
    val email: String, 
    val name: String, 
    val role: String, 
    val token: String?,
    val passwordChangeRequired: Boolean = false
)

data class CsrfTokenResponse(
    val token: String,
    val headerName: String = "X-XSRF-TOKEN",
    val parameterName: String = "_csrf"
)

@RestController
@RequestMapping("${ApiVersionConfig.API_VERSION}/admin/auth")
@Tag(name = "Admin Authentication", description = "Admin authentication endpoints")
@CrossOrigin(
    origins = ["http://localhost:3000", "http://localhost:8080", "https://yourcompany.com"],
    allowCredentials = "true",
    allowedHeaders = ["Content-Type", "Authorization", "X-Requested-With"],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS]
)
class AdminAuthController(
    private val adminService: AdminService,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(AdminAuthController::class.java)
    
    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate admin user and set JWT token as httpOnly cookie")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ApiResponse(responseCode = "400", description = "Invalid request format")
    ])
    fun login(@Valid @RequestBody request: LoginRequest, response: HttpServletResponse): ResponseEntity<LoginResponse> {
        // AdminService.authenticate now throws InvalidCredentialsException on failure
        val adminUser = adminService.authenticate(request.email, request.password)
        
        // Generate JWT token
        val token = jwtService.generateToken(
            email = adminUser.email,
            userId = adminUser.id!!,
            role = adminUser.role.name
        )
        
        // Set JWT as httpOnly cookie
        val cookie = Cookie("jwt_token", token).apply {
            isHttpOnly = true
            secure = true // Use HTTPS in production
            path = "/"
            maxAge = 24 * 60 * 60 // 24 hours
        }
        response.addCookie(cookie)
        
        return ResponseEntity.ok(LoginResponse(
            id = adminUser.id.toString(),
            email = adminUser.email,
            name = adminUser.name,
            role = adminUser.role.name.lowercase(),
            token = null, // Don't send token in response body
            passwordChangeRequired = adminUser.passwordChangeRequired
        ))
    }
    
    @PostMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Validate the current JWT token from cookie or header and return user information")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token is valid"),
        ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    ])
    fun validateToken(
        @RequestHeader("Authorization") authHeader: String?,
        request: HttpServletRequest
    ): ResponseEntity<LoginResponse> {
        // Try to get token from cookie first, then fallback to Authorization header
        val token = getTokenFromRequest(request, authHeader)
        
        if (token == null) {
            return ResponseEntity.status(401).build()
        }
        
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
                token = null, // Don't regenerate token on validation
                passwordChangeRequired = adminUser.passwordChangeRequired
            ))
        } else {
            ResponseEntity.status(401).build()
        }
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "Change admin password", description = "Change the password for an authenticated admin user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password changed successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request or password validation failed"),
        ApiResponse(responseCode = "401", description = "Unauthorized - invalid token or current password")
    ])
    fun changePassword(
        @RequestHeader("Authorization") authHeader: String?,
        @Valid @RequestBody passwordRequest: PasswordChangeRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Map<String, String>> {
        val token = getTokenFromRequest(httpRequest, authHeader)
        
        if (token == null) {
            return ResponseEntity.status(401).body(mapOf("error" to "Authentication required"))
        }
        
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid token"))
        }
        
        // Validate password confirmation
        if (passwordRequest.newPassword != passwordRequest.confirmPassword) {
            return ResponseEntity.badRequest().body(mapOf("error" to "New password and confirmation do not match"))
        }
        
        val userId = jwtService.getUserIdFromToken(token)
        if (userId == null) {
            return ResponseEntity.status(401).body(mapOf("error" to "Invalid token"))
        }
        
        // AdminService.changePassword now throws appropriate exceptions
        adminService.changePassword(userId, passwordRequest.currentPassword, passwordRequest.newPassword)
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }
    
    @GetMapping("/csrf-token")
    @Operation(summary = "Get CSRF token", description = "Retrieve CSRF token for secure state-changing requests")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "CSRF token retrieved successfully"),
        ApiResponse(responseCode = "500", description = "Failed to generate CSRF token")
    ])
    fun getCsrfToken(request: HttpServletRequest): ResponseEntity<CsrfTokenResponse> {
        val csrfToken = request.getAttribute(CsrfToken::class.java.name) as? CsrfToken
        return if (csrfToken != null) {
            ResponseEntity.ok(CsrfTokenResponse(
                token = csrfToken.token,
                headerName = csrfToken.headerName,
                parameterName = csrfToken.parameterName
            ))
        } else {
            logger.error("Failed to retrieve CSRF token from request")
            ResponseEntity.status(500).build()
        }
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Admin logout", description = "Logout the current admin user and clear JWT cookie")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully logged out")
    ])
    fun logout(response: HttpServletResponse): ResponseEntity<Map<String, String>> {
        // Clear the JWT cookie
        val cookie = Cookie("jwt_token", "").apply {
            isHttpOnly = true
            secure = true
            path = "/"
            maxAge = 0 // Delete the cookie
        }
        response.addCookie(cookie)
        
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
    
    // OPTIONS handler for CORS preflight
    @RequestMapping("/login", method = [RequestMethod.OPTIONS])
    fun loginOptions(): ResponseEntity<Void> {
        logger.debug("OPTIONS request for /auth/login")
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "POST, OPTIONS")
            .header("Access-Control-Allow-Headers", "Content-Type")
            .build()
    }
    
    /**
     * Helper method to extract JWT token from cookie or Authorization header
     */
    private fun getTokenFromRequest(request: HttpServletRequest, authHeader: String?): String? {
        // First, try to get token from cookie
        request.cookies?.let { cookies ->
            for (cookie in cookies) {
                if (cookie.name == "jwt_token") {
                    return cookie.value
                }
            }
        }
        
        // Fallback to Authorization header for backward compatibility
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }
        
        return null
    }
}