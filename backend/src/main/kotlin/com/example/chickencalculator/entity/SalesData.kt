package com.example.chickencalculator.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(
    name = "sales_data",
    indexes = [
        Index(name = "idx_sales_date", columnList = "date"),
        Index(name = "idx_sales_location", columnList = "location_id"),
        Index(name = "idx_sales_date_location", columnList = "date, location_id"),
        Index(name = "idx_sales_date_range", columnList = "location_id, date DESC")
    ],
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["date", "location_id"])
    ]
)
data class SalesData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    @field:NotNull
    val date: LocalDate,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val totalSales: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val portionsSoy: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val portionsTeriyaki: BigDecimal,
    
    @Column(nullable = false, precision = 10, scale = 2)
    @field:NotNull
    @field:DecimalMin(value = "0.0", inclusive = true)
    val portionsTurmeric: BigDecimal,
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    @field:NotNull(message = "Location is required")
    val location: Location
)