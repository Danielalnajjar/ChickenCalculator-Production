package com.example.chickencalculator.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "marination_log",
    indexes = [
        Index(name = "idx_marination_timestamp", columnList = "timestamp"),
        Index(name = "idx_marination_location", columnList = "location_id")
    ]
)
data class MarinationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
    @SequenceGenerator(name = "entity_seq", sequenceName = "entity_id_seq", allocationSize = 1)
    val id: Long = 0,
    
    @Column(nullable = false)
    @field:NotNull
    val timestamp: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val soySuggested: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val teriyakiSuggested: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val turmericSuggested: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val soyPans: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val teriyakiPans: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val turmericPans: BigDecimal,
    
    @Column(nullable = false)
    @field:NotNull
    val isEndOfDay: Boolean = false,
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    @field:NotNull(message = "Location is required")
    val location: Location
)