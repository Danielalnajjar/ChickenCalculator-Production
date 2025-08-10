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
    val domain: String,
    
    val address: String? = null,
    
    @Column(nullable = false)
    val managerName: String,
    
    @Column(nullable = false)
    val managerEmail: String,
    
    @Enumerated(EnumType.STRING)
    val status: LocationStatus = LocationStatus.DEPLOYING,
    
    @Column(nullable = false)
    val cloudProvider: String,
    
    @Column(nullable = false)
    val region: String,
    
    val serverIp: String? = null,
    
    val databaseUrl: String? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val deployedAt: LocalDateTime? = null,
    
    val lastSeenAt: LocalDateTime? = null,
    
    val deploymentLogs: String? = null
)

enum class LocationStatus {
    DEPLOYING, ACTIVE, ERROR, MAINTENANCE, DELETED
}