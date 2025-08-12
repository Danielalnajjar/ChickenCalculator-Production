package com.example.chickencalculator.controller

import com.example.chickencalculator.service.LocationService
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
    private val locationService: LocationService
) {
    private val logger = LoggerFactory.getLogger(LocationSlugController::class.java)
    
    /**
     * Handle location-specific routes by slug
     * This allows accessing calculators via URLs like /fashion-show, /downtown-store, etc.
     */
    @GetMapping("/{slug}")
    fun serveLocationBySlug(@PathVariable slug: String): ResponseEntity<Resource> {
        logger.info("🔍 Checking slug: $slug")
        
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
        val location = locationService.getLocationBySlug(slug)
        
        if (location != null) {
            logger.info("✅ Found location for slug: $slug -> ${location.name}")
            
            // Serve the main app's index.html for this location
            val fileResource = File("/app/static/app/index.html")
            if (fileResource.exists()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header("X-Location-Slug", slug)
                    .header("X-Location-Name", location.name)
                    .header("X-Location-Id", location.id.toString())
                    .body(FileSystemResource(fileResource))
            } else {
                logger.error("❌ Index.html not found at /app/static/app/index.html")
            }
        } else {
            logger.warn("⚠️ No location found for slug: $slug")
        }
        
        // Return 404 for invalid slugs
        return ResponseEntity.notFound().build()
    }
    
    /**
     * Handle sub-routes for location slugs (e.g., /fashion-show/calculator)
     */
    @GetMapping("/{slug}/{path}")
    fun serveLocationSubRoute(
        @PathVariable slug: String, 
        @PathVariable path: String
    ): ResponseEntity<Resource> {
        logger.info("🔍 Checking location sub-route: $slug/$path")
        
        // Check if this is a valid location slug
        val location = locationService.getLocationBySlug(slug)
        
        if (location != null) {
            logger.info("✅ Found location for slug: $slug -> ${location.name}, serving path: $path")
            
            // Serve the main app's index.html for React Router to handle
            val fileResource = File("/app/static/app/index.html")
            if (fileResource.exists()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header("X-Location-Slug", slug)
                    .header("X-Location-Name", location.name)
                    .header("X-Location-Id", location.id.toString())
                    .body(FileSystemResource(fileResource))
            }
        }
        
        return ResponseEntity.notFound().build()
    }
}