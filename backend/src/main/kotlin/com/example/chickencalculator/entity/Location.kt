package com.example.chickencalculator.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "locations")
data class Location(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
    @SequenceGenerator(name = "entity_seq", sequenceName = "entity_id_seq", allocationSize = 1)
    val id: Long = 0,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(unique = true, nullable = false)
    val slug: String,  // Auto-generated from name, e.g., "las-vegas-store"
    
    val address: String? = null,
    
    @Column(nullable = false)
    val managerName: String,
    
    @Column(nullable = false)
    val managerEmail: String,
    
    @Enumerated(EnumType.STRING)
    val status: LocationStatus = LocationStatus.ACTIVE,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val isDefault: Boolean = false,  // Will be removed in migration V5
    
    // Authentication fields for location-specific access
    @Column(name = "password_hash")
    val passwordHash: String? = null,
    
    @Column(name = "requires_auth", nullable = false)
    val requiresAuth: Boolean = true,
    
    @Column(name = "session_timeout_hours", nullable = false)
    val sessionTimeoutHours: Int = 8,
    
    @Column(name = "last_password_change")
    val lastPasswordChange: LocalDateTime? = null,
    
    @Column(name = "failed_login_attempts", nullable = false)
    val failedLoginAttempts: Int = 0,
    
    @Column(name = "last_failed_login")
    val lastFailedLogin: LocalDateTime? = null
)

enum class LocationStatus {
    ACTIVE, INACTIVE
}