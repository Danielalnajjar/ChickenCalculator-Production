package com.example.chickencalculator.controller

import com.example.chickencalculator.service.LocationManagementService
import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@RestController
class LocationSlugController(
    private val locationManagementService: LocationManagementService,
    private val metricsService: MetricsService
) {
    private val logger = LoggerFactory.getLogger(LocationSlugController::class.java)
    
    /**
     * Get location information by slug for API clients
     * Returns location metadata without serving HTML files
     */
    @GetMapping("/api/v1/location/{slug}/info")
    @Timed(value = "chicken.calculator.location.slug.time", description = "Time taken to get location info by slug")
    fun getLocationInfo(@PathVariable slug: String): ResponseEntity<Map<String, Any>> {
        val startTime = System.currentTimeMillis()
        logger.info("🔍 Getting location info for slug: $slug")
        
        try {
            // Check if this is a valid location slug
            val lookupStartTime = System.currentTimeMillis()
            val location = locationManagementService.getLocationBySlug(slug)
            val lookupTime = System.currentTimeMillis() - lookupStartTime
            
            if (location != null) {
                logger.info("✅ Found location for slug: $slug -> ${location.name}")
                
                // Record successful location access
                metricsService.recordLocationLookup(slug, lookupTime, true)
                metricsService.recordLocationAccess(slug, "info")
                
                val totalTime = System.currentTimeMillis() - startTime
                metricsService.recordDatabaseOperation("location_slug_lookup", totalTime)
                
                val locationInfo = mapOf(
                    "id" to location.id,
                    "slug" to location.slug,
                    "name" to location.name,
                    "managerName" to location.managerName,
                    "managerEmail" to location.managerEmail,
                    "createdAt" to location.createdAt
                )
                
                return ResponseEntity.ok(locationInfo)
            } else {
                logger.warn("⚠️ No location found for slug: $slug")
                metricsService.recordLocationLookup(slug, lookupTime, false)
                metricsService.recordError("location_slug_not_found", "LocationNotFoundException", slug)
                return ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            metricsService.recordError("location_slug_lookup", e.javaClass.simpleName, slug)
            throw e
        }
    }
    
    /**
     * Validate that a location slug exists (lightweight endpoint)
     */
    @GetMapping("/api/v1/location/{slug}/validate")
    @Timed(value = "chicken.calculator.location.validate.time", description = "Time taken to validate location slug")
    fun validateLocationSlug(@PathVariable slug: String): ResponseEntity<Map<String, Any>> {
        val startTime = System.currentTimeMillis()
        logger.info("🔍 Validating location slug: $slug")
        
        try {
            val lookupStartTime = System.currentTimeMillis()
            val location = locationManagementService.getLocationBySlug(slug)
            val lookupTime = System.currentTimeMillis() - lookupStartTime
            
            if (location != null) {
                logger.info("✅ Validated location slug: $slug")
                metricsService.recordLocationLookup(slug, lookupTime, true)
                
                val totalTime = System.currentTimeMillis() - startTime
                metricsService.recordDatabaseOperation("location_slug_validate", totalTime)
                
                val response = mapOf(
                    "valid" to true,
                    "slug" to slug,
                    "name" to location.name
                )
                
                return ResponseEntity.ok(response)
            } else {
                logger.warn("⚠️ Invalid location slug: $slug")
                metricsService.recordLocationLookup(slug, lookupTime, false)
                
                val response = mapOf(
                    "valid" to false,
                    "slug" to slug
                )
                
                return ResponseEntity.ok(response)
            }
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            metricsService.recordError("location_slug_validate", e.javaClass.simpleName, slug)
            throw e
        }
    }
}