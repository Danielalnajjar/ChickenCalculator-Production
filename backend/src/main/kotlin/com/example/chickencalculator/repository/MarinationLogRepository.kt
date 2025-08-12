package com.example.chickencalculator.repository

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.MarinationLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface MarinationLogRepository : JpaRepository<MarinationLog, Long> {
    
    // Location-based queries for multi-tenancy
    fun findByLocationOrderByTimestampDesc(location: Location): List<MarinationLog>
    
    fun findByLocationAndTimestampBetween(
        location: Location,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<MarinationLog>
    
    @Query("""
        SELECT m FROM MarinationLog m 
        WHERE m.location = :location
        AND DATE(m.timestamp) = :date 
        ORDER BY m.timestamp DESC
    """)
    fun findByLocationAndDate(
        @Param("location") location: Location,
        @Param("date") date: LocalDate
    ): List<MarinationLog>
    
    // Legacy methods (consider deprecating)
    @Deprecated("Use findByLocationOrderByTimestampDesc for multi-tenancy")
    fun findAllByOrderByTimestampDesc(): List<MarinationLog>
    
    @Query("""
        SELECT m FROM MarinationLog m 
        WHERE DATE(m.timestamp) = :date 
        ORDER BY m.timestamp DESC
    """)
    @Deprecated("Use findByLocationAndDate for multi-tenancy")
    fun findByDate(@Param("date") date: LocalDate): List<MarinationLog>
    
    @Query("""
        SELECT m FROM MarinationLog m 
        WHERE m.timestamp >= :startOfDay AND m.timestamp < :endOfDay 
        AND m.isEndOfDay = false
        ORDER BY m.timestamp DESC
    """)
    @Deprecated("Use findByLocationAndTimestampBetween for multi-tenancy")
    fun findTodaysMarinationLogs(
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime
    ): List<MarinationLog>
    
    // Additional methods for service layer support
    fun findByLocationAndIsEndOfDayTrueOrderByTimestampDesc(location: Location): List<MarinationLog>
    
    fun deleteByLocation(location: Location): Long
}