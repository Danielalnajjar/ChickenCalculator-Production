package com.example.chickencalculator.service

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
import com.example.chickencalculator.repository.AdminUserRepository
import org.springframework.stereotype.Service
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.security.SecureRandom

@Service
class AdminService(private val adminUserRepository: AdminUserRepository) {
    
    private val passwordEncoder = BCryptPasswordEncoder(12) // Strong bcrypt rounds
    
    fun authenticate(email: String, password: String): AdminUser? {
        val user = adminUserRepository.findByEmail(email)
        return if (user != null && verifyPassword(password, user.passwordHash)) {
            // Update last login time
            val updatedUser = user.copy(lastLoginAt = LocalDateTime.now())
            adminUserRepository.save(updatedUser)
            updatedUser
        } else {
            null
        }
    }
    
    fun createAdminUser(email: String, password: String, name: String, role: AdminRole): AdminUser {
        val passwordHash = hashPassword(password)
        val adminUser = AdminUser(
            email = email,
            passwordHash = passwordHash,
            name = name,
            role = role
        )
        return adminUserRepository.save(adminUser)
    }
    
    private fun hashPassword(password: String): String {
        validatePassword(password) // Validate before hashing
        return passwordEncoder.encode(password)
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return passwordEncoder.matches(password, hash)
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
    fun initializeDefaultAdmin() {
        if (adminUserRepository.count() == 0L) {
            val defaultPassword = System.getenv("ADMIN_DEFAULT_PASSWORD") 
                ?: generateSecurePassword()
            
            createAdminUser(
                email = System.getenv("ADMIN_DEFAULT_EMAIL") ?: "admin@yourcompany.com",
                password = defaultPassword,
                name = "System Administrator",
                role = AdminRole.ADMIN
            )
            
            // Log the generated password in production (only once)
            if (System.getenv("ADMIN_DEFAULT_PASSWORD") == null) {
                println("🔐 DEFAULT ADMIN PASSWORD GENERATED: $defaultPassword")
                println("⚠️  CHANGE THIS PASSWORD IMMEDIATELY AFTER FIRST LOGIN!")
            }
        }
    }
    
    private fun generateSecurePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..16)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}