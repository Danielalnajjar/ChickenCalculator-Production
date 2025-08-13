package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminPortalController @Autowired constructor(
    private val resourceLoader: ResourceLoader
) {
    private val logger = LoggerFactory.getLogger(AdminPortalController::class.java)
    
    @Value("\${admin.portal.path:#{null}}")
    private val adminPortalPath: String? = null
    
    @GetMapping("/admin", "/admin/")
    fun serveAdminPortal(): ResponseEntity<Resource> {
        logger.info("🌐 Serving admin portal index.html")
        
        // Try multiple resource locations in order of preference
        val resourcePaths = listOfNotNull(
            // Custom path from environment variable if set
            adminPortalPath?.let { "$it/index.html" },
            // Production deployment paths
            "file:/app/static/admin/index.html",
            "file:static/admin/index.html",
            // Classpath resources (for packaged JAR)
            "classpath:static/admin/index.html",
            "classpath:/static/admin/index.html",
            // Development paths
            "file:admin-portal/build/index.html",
            "file:../admin-portal/build/index.html"
        )
        
        for (path in resourcePaths) {
            try {
                val resource = resourceLoader.getResource(path)
                if (resource.exists() && resource.isReadable) {
                    logger.info("✅ Found admin portal at: $path")
                    return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(resource)
                }
            } catch (e: Exception) {
                logger.debug("Could not load resource from path: $path - ${e.message}")
            }
        }
        
        // If no resource found, return a helpful error page
        logger.error("❌ Admin portal index.html not found in any expected location")
        logger.error("Searched paths: $resourcePaths")
        
        val errorHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Admin Portal Configuration Error</title>
                <style>
                    body { font-family: system-ui, sans-serif; padding: 40px; max-width: 600px; margin: 0 auto; }
                    h1 { color: #d32f2f; }
                    pre { background: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto; }
                </style>
            </head>
            <body>
                <h1>Admin Portal Not Found</h1>
                <p>The admin portal static files could not be located. This is typically a deployment configuration issue.</p>
                <h3>Configuration Options:</h3>
                <ul>
                    <li>Set <code>admin.portal.path</code> in application properties</li>
                    <li>Ensure the admin portal is built: <code>cd admin-portal && npm run build</code></li>
                    <li>For production, files should be at <code>/app/static/admin/</code></li>
                </ul>
                <h3>Searched Locations:</h3>
                <pre>${resourcePaths.joinToString("\n")}</pre>
            </body>
            </html>
        """.trimIndent()
        
        // Use Spring's ByteArrayResource for the error page with proper override
        val errorResource = object : org.springframework.core.io.ByteArrayResource(errorHtml.toByteArray()) {
            override fun getFilename() = "admin-portal-error.html"
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.TEXT_HTML)
            .body(errorResource)
    }
    
    // Handle specific admin portal routes for React Router
    @GetMapping("/admin/login", "/admin/dashboard", "/admin/locations", "/admin/users", "/admin/reports", "/admin/settings")
    fun serveAdminPortalRoutes(): ResponseEntity<Resource> {
        logger.info("🌐 Admin portal route requested")
        // For React Router routes, serve the index.html
        return serveAdminPortal()
    }
}