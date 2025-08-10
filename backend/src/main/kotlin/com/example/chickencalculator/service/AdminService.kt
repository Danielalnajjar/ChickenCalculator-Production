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
    
    private val passwordEncoder = BCryptPasswordEncoder(10) // Reduced rounds for debugging
    
    fun authenticate(email: String, password: String): AdminUser? {
        println("🔐 Authentication attempt for email: $email")
        val user = adminUserRepository.findByEmail(email)
        
        if (user == null) {
            println("❌ No user found with email: $email")
            // List all admin emails for debugging
            val allAdmins = adminUserRepository.findAll()
            println("📧 Available admin emails: ${allAdmins.map { it.email }}")
            return null
        }
        
        println("✅ User found: ${user.email}, verifying password...")
        val passwordMatches = verifyPassword(password, user.passwordHash)
        println("🔑 Password verification result: $passwordMatches")
        
        return if (passwordMatches) {
            // Update last login time
            val updatedUser = user.copy(lastLoginAt = LocalDateTime.now())
            adminUserRepository.save(updatedUser)
            println("✅ Authentication successful for: ${user.email}")
            updatedUser
        } else {
            println("❌ Password verification failed for: ${user.email}")
            null
        }
    }
    
    fun createAdminUser(email: String, password: String, name: String, role: AdminRole): AdminUser {
        // TEMPORARY: Store plain password for debugging
        println("⚠️ WARNING: Storing password in plain text for debugging")
        println("📝 Creating user with email: $email and password: $password")
        
        val adminUser = AdminUser(
            email = email,
            passwordHash = password, // TEMPORARY: Plain text for debugging
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
        // TEMPORARY: Direct comparison for debugging
        println("🔍 Comparing password: '$password' with stored: '$hash'")
        return password == hash
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
        println("🔄 Checking for existing admin users...")
        val forceReset = System.getenv("FORCE_ADMIN_RESET") == "true"
        val adminCount = adminUserRepository.count()
        println("📊 Found $adminCount admin users in database")
        
        if (forceReset && adminCount > 0L) {
            println("⚠️ FORCE_ADMIN_RESET is true, deleting existing admin users...")
            adminUserRepository.deleteAll()
            println("🗑️ Deleted all existing admin users")
        }
        
        if (adminCount == 0L || forceReset) {
            val defaultEmail = System.getenv("ADMIN_DEFAULT_EMAIL") ?: "admin@yourcompany.com"
            // Use a simpler default password for initial deployment
            val defaultPassword = System.getenv("ADMIN_DEFAULT_PASSWORD") ?: "Admin123!"
            
            println("🔨 Creating default admin user with email: $defaultEmail")
            
            try {
                val adminUser = createAdminUser(
                    email = defaultEmail,
                    password = defaultPassword,
                    name = "System Administrator",
                    role = AdminRole.ADMIN
                )
                
                println("✅ Admin user created successfully with ID: ${adminUser.id}")
                
                // Always log credentials in production for debugging
                println("=".repeat(60))
                println("🔐 DEFAULT ADMIN CREDENTIALS:")
                println("📧 Email: $defaultEmail")
                println("🔑 Password: $defaultPassword")
                println("⚠️  CHANGE THIS PASSWORD IMMEDIATELY AFTER FIRST LOGIN!")
                println("=".repeat(60))
                
                // Test authentication immediately
                println("🧪 Testing authentication with created credentials...")
                val testAuth = authenticate(defaultEmail, defaultPassword)
                if (testAuth != null) {
                    println("✅ Authentication test successful!")
                } else {
                    println("❌ Authentication test failed! Check password encoder.")
                }
                
            } catch (e: Exception) {
                println("❌ Error creating admin user: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("ℹ️ Admin users already exist, skipping creation")
            // Show existing admin emails for debugging
            val allAdmins = adminUserRepository.findAll()
            println("📧 Existing admin emails: ${allAdmins.map { it.email }}")
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
}