package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.io.File

@Controller
class AdminPortalController {
    private val logger = LoggerFactory.getLogger(AdminPortalController::class.java)
    
    @GetMapping("/admin", "/admin/")
    fun serveAdminPortal(): ResponseEntity<Any> {
        logger.info("🌐 Serving admin portal index.html")
        
        // Try filesystem location first (Docker deployment)
        val fileResource = File("/app/static/admin/index.html")
        if (fileResource.exists()) {
            logger.info("✅ Found admin portal at: ${fileResource.absolutePath}")
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(FileSystemResource(fileResource))
        }
        
        // Log what files we can find
        val adminDir = File("/app/static/admin")
        if (adminDir.exists()) {
            logger.info("📂 Admin directory exists. Files: ${adminDir.listFiles()?.map { it.name }?.joinToString()}")
        } else {
            logger.error("❌ Admin directory does not exist at /app/static/admin")
        }
        
        // Return error with debugging info
        val errorHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Admin Portal Error</title></head>
            <body>
                <h1>Admin Portal Not Found</h1>
                <p>Looking for: /app/static/admin/index.html</p>
                <p>Directory exists: ${adminDir.exists()}</p>
                <p>Files in directory: ${if (adminDir.exists()) adminDir.listFiles()?.map { it.name }?.joinToString() else "N/A"}</p>
            </body>
            </html>
        """.trimIndent()
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.TEXT_HTML)
            .body(errorHtml)
    }
    
    @GetMapping("/admin/{*path}")
    fun serveAdminPortalRoutes(@PathVariable path: String): ResponseEntity<Any> {
        logger.info("🌐 Admin route requested: $path")
        
        // Don't intercept static resource requests
        if (path.startsWith("static/") || path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".map")) {
            logger.info("   Skipping static resource: $path")
            return ResponseEntity.notFound().build()
        }
        
        // For all other admin routes, serve the index.html for React Router
        return serveAdminPortal()
    }
}