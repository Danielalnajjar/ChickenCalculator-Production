package com.example.chickencalculator.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "admin_users")
data class AdminUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val email: String,
    
    @Column(nullable = false)
    val passwordHash: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Enumerated(EnumType.STRING)
    val role: AdminRole = AdminRole.MANAGER,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val lastLoginAt: LocalDateTime? = null
)

enum class AdminRole {
    ADMIN, MANAGER
}