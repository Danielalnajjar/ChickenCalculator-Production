package com.example.chickencalculator.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "marination_log")
data class MarinationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val soySuggested: Double,
    
    @Column(nullable = false)
    val teriyakiSuggested: Double,
    
    @Column(nullable = false)
    val turmericSuggested: Double,
    
    @Column(nullable = false)
    val soyPans: Double,
    
    @Column(nullable = false)
    val teriyakiPans: Double,
    
    @Column(nullable = false)
    val turmericPans: Double,
    
    @Column(nullable = false)
    val isEndOfDay: Boolean = false
)