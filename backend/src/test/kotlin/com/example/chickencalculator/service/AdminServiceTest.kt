package com.example.chickencalculator.service

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.repository.AdminUserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    private lateinit var adminUserRepository: AdminUserRepository

    @Mock
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    @InjectMocks
    private lateinit var adminService: AdminService

    private lateinit var testAdmin: AdminUser

    @BeforeEach
    fun setup() {
        testAdmin = AdminUser(
            id = 1L,
            email = "admin@test.com",
            password = "\$2a\$10\$hashedpassword123",
            name = "Test Admin",
            role = "ADMIN"
        )
    }

    @Nested
    @DisplayName("Authentication Tests")
    inner class AuthenticationTests {

        @Test
        @DisplayName("Should authenticate valid credentials")
        fun testValidAuthentication() {
            // Given
            val email = "admin@test.com"
            val password = "ValidPassword123!"
            
            whenever(adminUserRepository.findByEmail(email))
                .thenReturn(testAdmin)
            whenever(passwordEncoder.matches(password, testAdmin.password))
                .thenReturn(true)

            // When
            val result = adminService.authenticate(email, password)

            // Then
            assertNotNull(result)
            assertEquals(testAdmin.email, result?.email)
            assertEquals(testAdmin.name, result?.name)
            verify(adminUserRepository).findByEmail(email)
            verify(passwordEncoder).matches(password, testAdmin.password)
        }

        @Test
        @DisplayName("Should reject invalid password")
        fun testInvalidPassword() {
            // Given
            val email = "admin@test.com"
            val wrongPassword = "WrongPassword"
            
            whenever(adminUserRepository.findByEmail(email))
                .thenReturn(testAdmin)
            whenever(passwordEncoder.matches(wrongPassword, testAdmin.password))
                .thenReturn(false)

            // When
            val result = adminService.authenticate(email, wrongPassword)

            // Then
            assertNull(result)
            verify(passwordEncoder).matches(wrongPassword, testAdmin.password)
        }

        @Test
        @DisplayName("Should reject non-existent user")
        fun testNonExistentUser() {
            // Given
            val email = "nonexistent@test.com"
            val password = "SomePassword"
            
            whenever(adminUserRepository.findByEmail(email))
                .thenReturn(null)

            // When
            val result = adminService.authenticate(email, password)

            // Then
            assertNull(result)
            verify(adminUserRepository).findByEmail(email)
            verify(passwordEncoder, never()).matches(any(), any())
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        fun testNullEmail() {
            // When
            val result = adminService.authenticate(null, "password")

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle empty credentials")
        fun testEmptyCredentials() {
            // When
            val result = adminService.authenticate("", "")

            // Then
            assertNull(result)
            verify(adminUserRepository, never()).findByEmail(any())
        }
    }

    @Nested
    @DisplayName("Admin User Management Tests")
    inner class AdminUserManagementTests {

        @Test
        @DisplayName("Should create new admin user")
        fun testCreateAdminUser() {
            // Given
            val email = "newadmin@test.com"
            val password = "NewPassword123!"
            val name = "New Admin"
            val hashedPassword = "\$2a\$10\$newhashed"
            
            whenever(passwordEncoder.encode(password))
                .thenReturn(hashedPassword)
            whenever(adminUserRepository.save(any<AdminUser>()))
                .thenAnswer { it.arguments[0] }

            // When
            val result = adminService.createAdminUser(email, password, name, "ADMIN")

            // Then
            assertNotNull(result)
            assertEquals(email, result.email)
            assertEquals(name, result.name)
            assertEquals(hashedPassword, result.password)
            verify(passwordEncoder).encode(password)
            verify(adminUserRepository).save(any())
        }

        @Test
        @DisplayName("Should find admin by email")
        fun testFindByEmail() {
            // Given
            val email = "admin@test.com"
            whenever(adminUserRepository.findByEmail(email))
                .thenReturn(testAdmin)

            // When
            val result = adminService.findByEmail(email)

            // Then
            assertNotNull(result)
            assertEquals(testAdmin.email, result?.email)
            verify(adminUserRepository).findByEmail(email)
        }

        @Test
        @DisplayName("Should find admin by ID")
        fun testFindById() {
            // Given
            val id = 1L
            whenever(adminUserRepository.findById(id))
                .thenReturn(Optional.of(testAdmin))

            // When
            val result = adminService.findById(id)

            // Then
            assertNotNull(result)
            assertEquals(testAdmin.id, result?.id)
            verify(adminUserRepository).findById(id)
        }

        @Test
        @DisplayName("Should return null for non-existent ID")
        fun testFindByIdNotFound() {
            // Given
            val id = 999L
            whenever(adminUserRepository.findById(id))
                .thenReturn(Optional.empty())

            // When
            val result = adminService.findById(id)

            // Then
            assertNull(result)
            verify(adminUserRepository).findById(id)
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    inner class PasswordManagementTests {

        @Test
        @DisplayName("Should update password with proper encoding")
        fun testUpdatePassword() {
            // Given
            val userId = 1L
            val newPassword = "NewSecurePassword123!"
            val hashedPassword = "\$2a\$10\$newhashedpassword"
            
            whenever(adminUserRepository.findById(userId))
                .thenReturn(Optional.of(testAdmin))
            whenever(passwordEncoder.encode(newPassword))
                .thenReturn(hashedPassword)
            whenever(adminUserRepository.save(any<AdminUser>()))
                .thenAnswer { it.arguments[0] }

            // When
            val result = adminService.updatePassword(userId, newPassword)

            // Then
            assertTrue(result)
            assertEquals(hashedPassword, testAdmin.password)
            verify(passwordEncoder).encode(newPassword)
            verify(adminUserRepository).save(testAdmin)
        }

        @Test
        @DisplayName("Should not update password for non-existent user")
        fun testUpdatePasswordUserNotFound() {
            // Given
            val userId = 999L
            val newPassword = "NewPassword123!"
            
            whenever(adminUserRepository.findById(userId))
                .thenReturn(Optional.empty())

            // When
            val result = adminService.updatePassword(userId, newPassword)

            // Then
            assertFalse(result)
            verify(passwordEncoder, never()).encode(any())
            verify(adminUserRepository, never()).save(any())
        }

        @Test
        @DisplayName("Should validate password strength")
        fun testPasswordStrengthValidation() {
            // Test various password patterns
            assertTrue(adminService.isPasswordValid("ValidPass123!"))
            assertFalse(adminService.isPasswordValid("short"))
            assertFalse(adminService.isPasswordValid("nouppercase123!"))
            assertFalse(adminService.isPasswordValid("NOLOWERCASE123!"))
            assertFalse(adminService.isPasswordValid("NoNumbers!"))
            assertFalse(adminService.isPasswordValid("NoSpecialChar123"))
        }
    }

    @Nested
    @DisplayName("Admin Initialization Tests")
    inner class AdminInitializationTests {

        @Test
        @DisplayName("Should initialize default admin when none exists")
        fun testInitializeDefaultAdmin() {
            // Given
            whenever(adminUserRepository.count()).thenReturn(0L)
            whenever(passwordEncoder.encode(any())).thenReturn("\$2a\$10\$defaulthashed")
            whenever(adminUserRepository.save(any<AdminUser>())).thenAnswer { it.arguments[0] }

            // When
            adminService.initializeDefaultAdminIfNeeded()

            // Then
            verify(adminUserRepository).count()
            verify(adminUserRepository).save(argThat<AdminUser> { 
                email == "admin@yourcompany.com" && role == "ADMIN"
            })
        }

        @Test
        @DisplayName("Should not initialize admin when one exists")
        fun testSkipInitializationWhenAdminExists() {
            // Given
            whenever(adminUserRepository.count()).thenReturn(1L)

            // When
            adminService.initializeDefaultAdminIfNeeded()

            // Then
            verify(adminUserRepository).count()
            verify(adminUserRepository, never()).save(any())
        }
    }
}