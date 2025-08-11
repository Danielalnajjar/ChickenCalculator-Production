package com.example.chickencalculator.repository

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationRepository : JpaRepository<Location, Long> {
    fun findBySlug(slug: String): Location?
    fun findByStatus(status: LocationStatus): List<Location>
    fun findByManagerEmail(managerEmail: String): List<Location>
    fun findByIsDefaultTrue(): Location?
}