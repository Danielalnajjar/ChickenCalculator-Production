package com.example.chickencalculator.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "locations")
data class Location(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    val isDefault: Boolean = false  // Flag for the default/main calculator location
)

enum class LocationStatus {
    ACTIVE, INACTIVE
}