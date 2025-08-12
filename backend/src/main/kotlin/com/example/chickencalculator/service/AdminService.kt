package com.example.chickencalculator.service

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.repository.AdminUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.security.SecureRandom

@Service
class AdminService(private val adminUserRepository: AdminUserRepository) {
    private val logger = LoggerFactory.getLogger(AdminService::class.java)
    private val passwordEncoder = BCryptPasswordEncoder(10)
    
    @Transactional
    fun authenticate(email: String, password: String): AdminUser? {
        logger.debug("Authentication attempt for email: {}", email)
        val user = adminUserRepository.findByEmail(email)
        
        if (user == null) {
            logger.warn("No user found with email: {}", email)
            return null
        }
        
        logger.debug("User found: {}, verifying password", user.email)
        val passwordMatches = verifyPassword(password, user.passwordHash)
        
        return if (passwordMatches) {
            // Update last login time
            val updatedUser = user.copy(lastLoginAt = LocalDateTime.now())
            adminUserRepository.save(updatedUser)
            logger.info("Authentication successful for: {}", user.email)
            updatedUser
        } else {
            logger.warn("Password verification failed for: {}", user.email)
            null
        }
    }
    
    @Transactional
    fun createAdminUser(email: String, password: String, name: String, role: AdminRole, passwordChangeRequired: Boolean = false): AdminUser {
        validatePassword(password) // Validate before hashing
        
        val adminUser = AdminUser(
            email = email,
            passwordHash = hashPassword(password), // Use proper BCrypt hashing
            name = name,
            role = role,
            passwordChangeRequired = passwordChangeRequired
        )
        return adminUserRepository.save(adminUser)
    }
    
    private fun hashPassword(password: String): String {
        validatePassword(password) // Validate before hashing
        return passwordEncoder.encode(password)
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            passwordEncoder.matches(password, hash)
        } catch (e: Exception) {
            // Handle legacy plain text passwords during migration
            false
        }
    }
    
    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters long")
        }
        if (!password.any { it.isUpperCase() }) {
            throw IllegalArgumentException("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            throw IllegalArgumentException("Password must contain at least one lowercase letter")  
        }
        if (!password.any { it.isDigit() }) {
            throw IllegalArgumentException("Password must contain at least one number")
        }
        if (password.contains(" ")) {
            throw IllegalArgumentException("Password cannot contain spaces")
        }
    }
    
    // Initialize default admin user if none exists
    @Transactional
    fun initializeDefaultAdmin() {
        logger.info("Checking for existing admin users")
        val forceReset = System.getenv("FORCE_ADMIN_RESET") == "true"
        val adminCount = adminUserRepository.count()
        logger.info("Found {} admin users in database", adminCount)
        
        if (forceReset && adminCount > 0L) {
            logger.warn("FORCE_ADMIN_RESET is true, deleting existing admin users")
            adminUserRepository.deleteAll()
            logger.info("Deleted all existing admin users")
        }
        
        if (adminCount == 0L || forceReset) {
            val defaultEmail = System.getenv("ADMIN_DEFAULT_EMAIL") ?: "admin@yourcompany.com"
            val defaultPassword = System.getenv("ADMIN_DEFAULT_PASSWORD") ?: "Admin123!"
            
            logger.info("Creating default admin user with email: {}", defaultEmail)
            
            try {
                val adminUser = createAdminUser(
                    email = defaultEmail,
                    password = defaultPassword,
                    name = "System Administrator",
                    role = AdminRole.ADMIN,
                    passwordChangeRequired = true // Force password change for default admin
                )
                
                logger.info("Admin user created successfully with ID: {}", adminUser.id)
                logger.warn("Default admin created with email: {}. CHANGE PASSWORD IMMEDIATELY!", defaultEmail)
                
                // Test authentication
                val testAuth = authenticate(defaultEmail, defaultPassword)
                if (testAuth != null) {
                    logger.info("Authentication test successful")
                } else {
                    logger.error("Authentication test failed! Check password encoder")
                }
                
            } catch (e: Exception) {
                logger.error("Error creating admin user", e)
            }
        } else {
            logger.info("Admin users already exist, skipping creation")
        }
    }
    
    private fun generateSecurePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..16)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    // Helper methods for debugging
    fun getAdminCount(): Long {
        return adminUserRepository.count()
    }
    
    fun getAllAdminEmails(): List<String> {
        return adminUserRepository.findAll().map { it.email }
    }
    
    fun getAdminByEmail(email: String): AdminUser? {
        return adminUserRepository.findByEmail(email)
    }
    
    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String): Boolean {
        val user = adminUserRepository.findById(userId).orElse(null) ?: return false
        
        // Verify current password
        if (!verifyPassword(currentPassword, user.passwordHash)) {
            logger.warn("Password change attempt with invalid current password for user: {}", user.email)
            return false
        }
        
        // Validate new password
        try {
            validatePassword(newPassword)
        } catch (e: IllegalArgumentException) {
            logger.warn("Password change attempt with invalid new password for user: {}: {}", user.email, e.message)
            throw e
        }
        
        // Update password and clear password change requirement
        val updatedUser = user.copy(
            passwordHash = hashPassword(newPassword),
            passwordChangeRequired = false
        )
        
        adminUserRepository.save(updatedUser)
        logger.info("Password changed successfully for user: {}", user.email)
        return true
    }
}