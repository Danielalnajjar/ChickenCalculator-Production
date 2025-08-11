package com.example.chickencalculator.repository

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.SalesData
import com.example.chickencalculator.model.SalesTotals
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SalesDataRepository : JpaRepository<SalesData, Long> {
    
    // Location-based queries for multi-tenancy
    fun findByLocationOrderByDateDesc(location: Location): List<SalesData>
    
    fun findByLocationIdOrderByDateDesc(locationId: Long): List<SalesData>
    
    fun findByLocationAndDateBetweenOrderByDateDesc(
        location: Location,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SalesData>
    
    fun findByLocationAndDate(location: Location, date: LocalDate): SalesData?
    
    // Paginated queries for large datasets
    fun findByLocation(location: Location, pageable: Pageable): Page<SalesData>
    
    @Query("""
        SELECT new com.example.chickencalculator.model.SalesTotals(
            CASE WHEN SUM(s.totalSales) IS NULL THEN 0.0 ELSE SUM(s.totalSales) END,
            CASE WHEN SUM(s.portionsSoy) IS NULL THEN 0.0 ELSE SUM(s.portionsSoy) END,
            CASE WHEN SUM(s.portionsTeriyaki) IS NULL THEN 0.0 ELSE SUM(s.portionsTeriyaki) END,
            CASE WHEN SUM(s.portionsTurmeric) IS NULL THEN 0.0 ELSE SUM(s.portionsTurmeric) END
        )
        FROM SalesData s
        WHERE s.location = :location
    """)
    fun getSalesTotalsByLocation(@Param("location") location: Location): SalesTotals
    
    @Query("""
        SELECT new com.example.chickencalculator.model.SalesTotals(
            CASE WHEN SUM(s.totalSales) IS NULL THEN 0.0 ELSE SUM(s.totalSales) END,
            CASE WHEN SUM(s.portionsSoy) IS NULL THEN 0.0 ELSE SUM(s.portionsSoy) END,
            CASE WHEN SUM(s.portionsTeriyaki) IS NULL THEN 0.0 ELSE SUM(s.portionsTeriyaki) END,
            CASE WHEN SUM(s.portionsTurmeric) IS NULL THEN 0.0 ELSE SUM(s.portionsTurmeric) END
        )
        FROM SalesData s
        WHERE s.location.id = :locationId
    """)
    fun getSalesTotalsByLocation(@Param("locationId") locationId: Long): SalesTotals
    
    @Query("""
        SELECT new com.example.chickencalculator.model.SalesTotals(
            CASE WHEN SUM(s.totalSales) IS NULL THEN 0.0 ELSE SUM(s.totalSales) END,
            CASE WHEN SUM(s.portionsSoy) IS NULL THEN 0.0 ELSE SUM(s.portionsSoy) END,
            CASE WHEN SUM(s.portionsTeriyaki) IS NULL THEN 0.0 ELSE SUM(s.portionsTeriyaki) END,
            CASE WHEN SUM(s.portionsTurmeric) IS NULL THEN 0.0 ELSE SUM(s.portionsTurmeric) END
        )
        FROM SalesData s
        WHERE s.location = :location
        AND s.date BETWEEN :startDate AND :endDate
    """)
    fun getSalesTotalsByLocationAndDateRange(
        @Param("location") location: Location,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): SalesTotals
    
    // Bulk operations
    fun deleteByLocationAndDateBefore(location: Location, date: LocalDate): Long
    
    fun deleteByLocationId(locationId: Long)
    
    // Legacy methods (consider deprecating)
    @Query("""
        SELECT new com.example.chickencalculator.model.SalesTotals(
            CASE WHEN SUM(s.totalSales) IS NULL THEN 0.0 ELSE SUM(s.totalSales) END,
            CASE WHEN SUM(s.portionsSoy) IS NULL THEN 0.0 ELSE SUM(s.portionsSoy) END,
            CASE WHEN SUM(s.portionsTeriyaki) IS NULL THEN 0.0 ELSE SUM(s.portionsTeriyaki) END,
            CASE WHEN SUM(s.portionsTurmeric) IS NULL THEN 0.0 ELSE SUM(s.portionsTurmeric) END
        )
        FROM SalesData s
    """)
    @Deprecated("Use getSalesTotalsByLocation instead for multi-tenancy")
    fun getSalesTotals(): SalesTotals
    
    @Deprecated("Use findByLocationOrderByDateDesc instead for multi-tenancy")
    fun findAllByOrderByDateDesc(): List<SalesData>
}