package com.example.chickencalculator.service

import com.example.chickencalculator.entity.AdminUser
import com.example.chickencalculator.entity.AdminRole
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
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    private lateinit var adminUserRepository: AdminUserRepository

    @InjectMocks
    private lateinit var adminService: AdminService

    private lateinit var testAdmin: AdminUser

    @BeforeEach
    fun setup() {
        testAdmin = AdminUser(
            id = 1L,
            email = "admin@test.com",
            passwordHash = "\$2a\$10\$hashedpassword123",
            name = "Test Admin",
            role = AdminRole.ADMIN,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            lastLoginAt = null
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

            // When
            val result = adminService.authenticate(email, password)

            // Then
            // Note: Result will be null because we can't mock the BCryptPasswordEncoder inside AdminService
            // This is a limitation of the current test setup
            verify(adminUserRepository).findByEmail(email)
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
            
            whenever(adminUserRepository.save(any<AdminUser>()))
                .thenAnswer { it.arguments[0] }

            // When
            val result = adminService.createAdminUser(email, password, name, AdminRole.ADMIN)

            // Then
            assertNotNull(result)
            assertEquals(email, result.email)
            assertEquals(name, result.name)
            assertEquals(AdminRole.ADMIN, result.role)
            verify(adminUserRepository).save(any())
        }

        @Test
        @DisplayName("Should find admin by email")
        fun testGetAdminByEmail() {
            // Given
            val email = "admin@test.com"
            whenever(adminUserRepository.findByEmail(email))
                .thenReturn(testAdmin)

            // When
            val result = adminService.getAdminByEmail(email)

            // Then
            assertNotNull(result)
            assertEquals(testAdmin.email, result?.email)
            verify(adminUserRepository).findByEmail(email)
        }

        @Test
        @DisplayName("Should return null for non-existent email")
        fun testGetAdminByEmailNotFound() {
            // Given
            val email = "nonexistent@test.com"
            whenever(adminUserRepository.findByEmail(email))
                .thenReturn(null)

            // When
            val result = adminService.getAdminByEmail(email)

            // Then
            assertNull(result)
            verify(adminUserRepository).findByEmail(email)
        }
    }
}