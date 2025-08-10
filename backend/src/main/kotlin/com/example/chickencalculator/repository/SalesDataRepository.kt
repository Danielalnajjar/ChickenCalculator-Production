package com.example.chickencalculator.repository

import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.SalesTotals
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SalesDataRepository : JpaRepository<SalesData, Long> {
    
    @Query("""
        SELECT new com.example.chickencalculator.model.SalesTotals(
            COALESCE(SUM(s.totalSales), 0.0),
            COALESCE(SUM(s.portionsSoy), 0.0),
            COALESCE(SUM(s.portionsTeriyaki), 0.0),
            COALESCE(SUM(s.portionsTurmeric), 0.0)
        )
        FROM SalesData s
    """)
    fun getSalesTotals(): SalesTotals
    
    fun findAllByOrderByDateDesc(): List<SalesData>
}