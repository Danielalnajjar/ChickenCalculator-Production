package com.example.chickencalculator.repository

import com.example.chickencalculator.entity.MarinationLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface MarinationLogRepository : JpaRepository<MarinationLog, Long> {
    
    fun findAllByOrderByTimestampDesc(): List<MarinationLog>
    
    @Query("""
        SELECT m FROM MarinationLog m 
        WHERE DATE(m.timestamp) = :date 
        ORDER BY m.timestamp DESC
    """)
    fun findByDate(@Param("date") date: LocalDate): List<MarinationLog>
    
    @Query("""
        SELECT m FROM MarinationLog m 
        WHERE m.timestamp >= :startOfDay AND m.timestamp < :endOfDay 
        AND m.isEndOfDay = false
        ORDER BY m.timestamp DESC
    """)
    fun findTodaysMarinationLogs(
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime
    ): List<MarinationLog>
}