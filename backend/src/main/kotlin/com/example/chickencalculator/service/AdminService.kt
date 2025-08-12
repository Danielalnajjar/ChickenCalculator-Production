package com.example.chickencalculator.service

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.exception.BusinessValidationException
import com.example.chickencalculator.exception.InvalidCredentialsException
import com.example.chickencalculator.repository.AdminUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Isolation
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.security.SecureRandom

@Service
class AdminService(private val adminUserRepository: AdminUserRepository) {
    private val logger = LoggerFactory.getLogger(AdminService::class.java)
    private val passwordEncoder = BCryptPasswordEncoder(10)
    
    @Transactional(rollbackFor = [Exception::class])
    fun authenticate(email: String, password: String): AdminUser {
        logger.info("Authentication attempt for email: {}", email)
        
        // Log all admin users for debugging
        val allAdmins = adminUserRepository.findAll()
        logger.info("Total admin users in database: {}", allAdmins.size)
        allAdmins.forEach { admin ->
            logger.info("Admin user: email={}, id={}, passwordChangeRequired={}", 
                admin.email, admin.id, admin.passwordChangeRequired)
        }
        
        val user = adminUserRepository.findByEmail(email)
        if (user == null) {
            logger.error("User not found in database: {}", email)
            logger.info("Available emails: {}", allAdmins.map { it.email })
            throw InvalidCredentialsException(email)
        }
        
        logger.info("User found: email={}, id={}, passwordHash starts with: {}", 
            user.email, user.id, user.passwordHash.take(10))
        
        val passwordMatches = verifyPassword(password, user.passwordHash)
        logger.info("Password verification result: {}", passwordMatches)
        
        if (!passwordMatches) {
            logger.error("Password verification failed for: {}", user.email)
            logger.info("Provided password length: {}, Hash length: {}", 
                password.length, user.passwordHash.length)
            throw InvalidCredentialsException(email)
        }
        
        // Update last login time
        val updatedUser = user.copy(lastLoginAt = LocalDateTime.now())
        adminUserRepository.save(updatedUser)
        logger.info("Authentication successful for: {}", user.email)
        return updatedUser
    }
    
    @Transactional(rollbackFor = [Exception::class])
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
        val errors = mutableMapOf<String, String>()
        
        if (password.length < 8) {
            errors["length"] = "Password must be at least 8 characters long"
        }
        if (!password.any { it.isUpperCase() }) {
            errors["uppercase"] = "Password must contain at least one uppercase letter"
        }
        if (!password.any { it.isLowerCase() }) {
            errors["lowercase"] = "Password must contain at least one lowercase letter"
        }
        if (!password.any { it.isDigit() }) {
            errors["digit"] = "Password must contain at least one number"
        }
        if (password.contains(" ")) {
            errors["spaces"] = "Password cannot contain spaces"
        }
        
        if (errors.isNotEmpty()) {
            throw BusinessValidationException("Password validation failed", errors)
        }
    }
    
    // Initialize default admin user if none exists
    @Transactional(rollbackFor = [Exception::class])
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
    @Transactional(readOnly = true)
    fun getAdminCount(): Long {
        return adminUserRepository.count()
    }
    
    @Transactional(readOnly = true)
    fun getAllAdminEmails(): List<String> {
        return adminUserRepository.findAll().map { it.email }
    }
    
    @Transactional(readOnly = true)
    fun getAdminByEmail(email: String): AdminUser? {
        return adminUserRepository.findByEmail(email)
    }
    
    @Transactional(rollbackFor = [Exception::class])
    fun changePassword(userId: Long, currentPassword: String, newPassword: String): Boolean {
        val user = adminUserRepository.findById(userId).orElseThrow {
            BusinessValidationException("User not found with ID: $userId")
        }
        
        // Verify current password
        if (!verifyPassword(currentPassword, user.passwordHash)) {
            logger.warn("Password change attempt with invalid current password for user: {}", user.email)
            throw InvalidCredentialsException("Current password is incorrect")
        }
        
        // Validate new password
        validatePassword(newPassword)
        
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