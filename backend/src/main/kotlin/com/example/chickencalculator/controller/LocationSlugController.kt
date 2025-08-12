package com.example.chickencalculator.controller

import com.example.chickencalculator.service.LocationService
import com.example.chickencalculator.service.MetricsService
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.io.File

@Controller
class LocationSlugController(
    private val locationService: LocationService,
    private val metricsService: MetricsService
) {
    private val logger = LoggerFactory.getLogger(LocationSlugController::class.java)
    
    /**
     * Handle location-specific routes by slug
     * This allows accessing calculators via URLs like /fashion-show, /downtown-store, etc.
     */
    @GetMapping("/{slug}")
    @Timed(value = "chicken.calculator.location.slug.time", description = "Time taken to serve location by slug")
    fun serveLocationBySlug(@PathVariable slug: String): ResponseEntity<Resource> {
        val startTime = System.currentTimeMillis()
        logger.info("🔍 Checking slug: $slug")
        
        try {
            // Skip known system paths
            val systemPaths = setOf(
                "api", "admin", "static", "resources", "actuator", 
                "favicon.ico", "manifest.json", "robots.txt",
                "calculator", "sales", "history", "settings"
            )
            
            if (systemPaths.contains(slug.lowercase())) {
                logger.debug("Skipping system path: $slug")
                return ResponseEntity.notFound().build()
            }
            
            // Check if this is a valid location slug
            val lookupStartTime = System.currentTimeMillis()
            val location = locationService.getLocationBySlug(slug)
            val lookupTime = System.currentTimeMillis() - lookupStartTime
            
            if (location != null) {
                logger.info("✅ Found location for slug: $slug -> ${location.name}")
                
                // Record successful location access
                metricsService.recordLocationLookup(slug, lookupTime, true)
                metricsService.recordLocationAccess(slug, "view")
                
                // Serve the main app's index.html for this location
                val fileResource = File("/app/static/app/index.html")
                if (fileResource.exists()) {
                    val totalTime = System.currentTimeMillis() - startTime
                    metricsService.recordDatabaseOperation("location_slug_lookup", totalTime)
                    
                    return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header("X-Location-Slug", slug)
                        .header("X-Location-Name", location.name)
                        .header("X-Location-Id", location.id.toString())
                        .body(FileSystemResource(fileResource))
                } else {
                    logger.error("❌ Index.html not found at /app/static/app/index.html")
                    metricsService.recordError("location_slug_file_not_found", "FileNotFoundException", slug)
                }
            } else {
                logger.warn("⚠️ No location found for slug: $slug")
                metricsService.recordLocationLookup(slug, lookupTime, false)
                metricsService.recordError("location_slug_not_found", "LocationNotFoundException", slug)
            }
            
            // Return 404 for invalid slugs
            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            metricsService.recordError("location_slug_lookup", e.javaClass.simpleName, slug)
            throw e
        }
    }
    
    /**
     * Handle sub-routes for location slugs (e.g., /fashion-show/calculator)
     */
    @GetMapping("/{slug}/{path}")
    @Timed(value = "chicken.calculator.location.subroute.time", description = "Time taken to serve location sub-route")
    fun serveLocationSubRoute(
        @PathVariable slug: String, 
        @PathVariable path: String
    ): ResponseEntity<Resource> {
        val startTime = System.currentTimeMillis()
        logger.info("🔍 Checking location sub-route: $slug/$path")
        
        // Skip admin routes - let AdminPortalController handle them
        if (slug.lowercase() == "admin") {
            logger.debug("Skipping admin route: $slug/$path")
            return ResponseEntity.notFound().build()
        }
        
        try {
            // Check if this is a valid location slug
            val lookupStartTime = System.currentTimeMillis()
            val location = locationService.getLocationBySlug(slug)
            val lookupTime = System.currentTimeMillis() - lookupStartTime
            
            if (location != null) {
                logger.info("✅ Found location for slug: $slug -> ${location.name}, serving path: $path")
                
                // Record successful location access with path
                metricsService.recordLocationLookup(slug, lookupTime, true)
                metricsService.recordLocationAccess(slug, "subroute")
                
                // Serve the main app's index.html for React Router to handle
                val fileResource = File("/app/static/app/index.html")
                if (fileResource.exists()) {
                    val totalTime = System.currentTimeMillis() - startTime
                    metricsService.recordDatabaseOperation("location_subroute_lookup", totalTime)
                    
                    return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header("X-Location-Slug", slug)
                        .header("X-Location-Name", location.name)
                        .header("X-Location-Id", location.id.toString())
                        .body(FileSystemResource(fileResource))
                } else {
                    metricsService.recordError("location_subroute_file_not_found", "FileNotFoundException", slug)
                }
            } else {
                metricsService.recordLocationLookup(slug, lookupTime, false)
                metricsService.recordError("location_subroute_not_found", "LocationNotFoundException", slug)
            }
            
            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            metricsService.recordError("location_subroute_lookup", e.javaClass.simpleName, slug)
            throw e
        }
    }
}