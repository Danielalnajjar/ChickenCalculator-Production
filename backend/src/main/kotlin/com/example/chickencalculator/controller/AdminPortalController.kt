package com.example.chickencalculator.controller

import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.io.File

@Controller
class AdminPortalController {
    
    @GetMapping("/admin", "/admin/")
    fun serveAdminPortal(): ResponseEntity<Resource> {
        // Try filesystem location first (Docker deployment)
        val fileResource = File("/app/static/admin/index.html")
        if (fileResource.exists()) {
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(FileSystemResource(fileResource))
        }
        
        // Fallback to error message
        return ResponseEntity.notFound().build()
    }
    
    @GetMapping("/admin/**")
    fun serveAdminPortalRoutes(): ResponseEntity<Resource> {
        // For all admin routes, serve the index.html for React Router
        return serveAdminPortal()
    }
}