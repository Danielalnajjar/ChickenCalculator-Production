package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.io.File

/**
 * Controller for serving the main application landing page.
 * Handles the root path "/" which displays the location selection page.
 */
@Controller
class MainController {
    private val logger = LoggerFactory.getLogger(MainController::class.java)
    
    @GetMapping("/test")
    fun test(): ResponseEntity<String> {
        return ResponseEntity.ok("Test endpoint works!")
    }
    
    /**
     * Serve the main application landing page at the root path.
     * This displays the location selection interface for multi-tenant access.
     */
    @GetMapping("/")
    fun serveLandingPage(): ResponseEntity<Resource> {
        logger.info("🏠 Serving main application landing page")
        
        // Check for the main app index.html file (same as LocationSlugController pattern)
        val fileResource = File("/app/static/app/index.html")
        if (fileResource.exists()) {
            logger.info("✅ Found main app at: /app/static/app/index.html")
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(FileSystemResource(fileResource))
        } else {
            logger.warn("⚠️ Main app index.html not found at /app/static/app/index.html")
        }
        
        // If no resource found, return a helpful error page
        logger.error("❌ Main application index.html not found in any expected location")
        
        val errorHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Application Not Found</title>
                <style>
                    body { 
                        font-family: system-ui, -apple-system, sans-serif; 
                        padding: 40px; 
                        max-width: 600px; 
                        margin: 0 auto;
                        background: #f5f5f5;
                    }
                    .container {
                        background: white;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    h1 { color: #d32f2f; margin-top: 0; }
                    pre { 
                        background: #f5f5f5; 
                        padding: 15px; 
                        border-radius: 4px; 
                        overflow-x: auto;
                        border: 1px solid #e0e0e0;
                    }
                    ul { line-height: 1.8; }
                    code {
                        background: #f5f5f5;
                        padding: 2px 6px;
                        border-radius: 3px;
                        font-family: 'Courier New', monospace;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Application Not Found</h1>
                    <p>The main application files could not be located. This is typically a deployment configuration issue.</p>
                    
                    <h3>For Administrators:</h3>
                    <ul>
                        <li>Ensure the frontend is built: <code>cd frontend && npm run build</code></li>
                        <li>For production, files should be at <code>/app/static/app/</code></li>
                        <li>Check the deployment logs for build errors</li>
                    </ul>
                    
                    <h3>Quick Links:</h3>
                    <ul>
                        <li><a href="/admin">Admin Portal</a></li>
                        <li><a href="/api/health">Health Check</a></li>
                        <li><a href="/actuator/health">Actuator Health</a></li>
                    </ul>
                    
                    <h3>Searched Location:</h3>
                    <pre>/app/static/app/index.html</pre>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        // Return 404 response without body
        return ResponseEntity.notFound().build()
    }
}