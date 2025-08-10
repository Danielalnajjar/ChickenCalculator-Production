package com.example.chickencalculator.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "sales_data")
data class SalesData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val date: LocalDate,
    
    @Column(nullable = false)
    val totalSales: Double,
    
    @Column(nullable = false)
    val portionsSoy: Double,
    
    @Column(nullable = false)
    val portionsTeriyaki: Double,
    
    @Column(nullable = false)
    val portionsTurmeric: Double
)