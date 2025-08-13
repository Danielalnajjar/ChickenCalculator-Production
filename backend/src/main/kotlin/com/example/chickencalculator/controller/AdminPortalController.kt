package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController

/**
 * AdminPortalController - Placeholder for admin-specific API endpoints
 * 
 * Note: Static file serving for the admin portal (HTML/JS/CSS) is now handled
 * by SpaController which uses Spring's Resource system properly.
 * 
 * This controller is reserved for future admin-specific API endpoints that are
 * not covered by AdminAuthController or AdminLocationController.
 * 
 * All file I/O operations have been removed to prevent servlet 500 errors.
 */
@RestController
class AdminPortalController {
    private val logger = LoggerFactory.getLogger(AdminPortalController::class.java)
    
    init {
        logger.info("AdminPortalController initialized - static file serving handled by SpaController")
    }
    
    // Future admin-specific API endpoints can be added here
    // Examples:
    // - GET /api/v1/admin/system-info
    // - GET /api/v1/admin/application-logs  
    // - POST /api/v1/admin/maintenance-mode
}