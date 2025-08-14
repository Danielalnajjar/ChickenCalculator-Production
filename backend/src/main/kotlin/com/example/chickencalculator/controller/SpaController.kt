package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SpaController(
    private val resourceLoader: ResourceLoader
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Admin portal routes - specific patterns only, no wildcards that could catch static resources
    @GetMapping(value = [
        "/admin",
        "/admin/",
        "/admin/login",
        "/admin/dashboard",
        "/admin/locations",
        "/admin/locations/{id}",
        "/admin/locations/{id}/edit",
        "/admin/settings",
        "/admin/settings/profile",
        "/admin/settings/security"
    ], produces = [MediaType.TEXT_HTML_VALUE])
    fun serveAdmin(): ResponseEntity<*> {
        return serveAdminIndex()
    }
    
    private fun serveAdminIndex(): ResponseEntity<*> {
        return try {
            val adminIndexResource = resourceLoader.getResource("file:/app/static/admin/index.html")
            if (!adminIndexResource.exists() || !adminIndexResource.isReadable) {
                logger.error("Admin index.html not found or not readable at /app/static/admin/index.html")
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body("Admin portal not found. Please ensure the frontend is built and deployed.")
            }
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(adminIndexResource)
        } catch (e: Exception) {
            logger.error("Error serving admin app", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_HTML)
                .body("Error loading admin portal: ${e.message}")
        }
    }

    // Location app routes - use multiple specific patterns to avoid issues
    @GetMapping(value = [
        "/location/{slug}",
        "/location/{slug}/",
        "/location/{slug}/{path1}",
        "/location/{slug}/{path1}/{path2}",
        "/location/{slug}/{path1}/{path2}/{path3}"
    ], produces = [MediaType.TEXT_HTML_VALUE])
    fun serveLocation(@PathVariable slug: String): ResponseEntity<*> {
        return serveLocationIndex(slug)
    }
    
    private fun serveLocationIndex(slug: String): ResponseEntity<*> {
        return try {
            val appIndexResource = resourceLoader.getResource("file:/app/static/app/index.html")
            if (!appIndexResource.exists() || !appIndexResource.isReadable) {
                logger.error("App index.html not found or not readable at /app/static/app/index.html")
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body("Location app not found. Please ensure the frontend is built and deployed.")
            }
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(appIndexResource)
        } catch (e: Exception) {
            logger.error("Error serving location app for slug: $slug", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_HTML)
                .body("Error loading location app: ${e.message}")
        }
    }
}