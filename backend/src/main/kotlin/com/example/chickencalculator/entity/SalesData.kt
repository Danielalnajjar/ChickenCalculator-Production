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
        Index(name = "idx_sales_location", columnList = "location_id")
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    val location: Location? = null
)