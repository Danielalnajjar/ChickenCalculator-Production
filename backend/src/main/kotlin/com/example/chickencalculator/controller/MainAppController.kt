package com.example.chickencalculator.controller

import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.io.File

@Controller
class MainAppController {
    
    @GetMapping("/", "/index.html")
    fun serveMainApp(): ResponseEntity<Resource> {
        // Try filesystem location (Docker deployment)
        val fileResource = File("/app/static/app/index.html")
        if (fileResource.exists()) {
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(FileSystemResource(fileResource))
        }
        
        // Fallback to error message
        return ResponseEntity.notFound().build()
    }
    
    // Handle React Router routes (but exclude /api and /admin)
    @GetMapping("/{path:[^\\.]*}")
    fun serveMainAppRoutes(): ResponseEntity<Resource> {
        // For all main app routes, serve the index.html for React Router
        return serveMainApp()
    }
}