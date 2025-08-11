package com.example.chickencalculator.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.File

@Configuration
class WebConfig : WebMvcConfigurer {
    private val logger = LoggerFactory.getLogger(WebConfig::class.java)
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        logger.info("🔧 Configuring static resource handlers...")
        
        // Debug: Check what files actually exist
        val adminStaticDir = File("/app/static/admin/static")
        if (adminStaticDir.exists()) {
            logger.info("📂 Admin static directory exists with ${adminStaticDir.listFiles()?.size ?: 0} items")
            adminStaticDir.listFiles()?.forEach { 
                logger.info("   - ${it.name} (${if (it.isDirectory) "directory" else "file"})")
            }
        } else {
            logger.warn("⚠️ Admin static directory not found at: /app/static/admin/static")
        }
        
        // Serve admin portal static assets (CSS, JS, images)
        // This should handle requests like /admin/static/js/main.js
        registry.addResourceHandler("/admin/static/**")
            .addResourceLocations("file:/app/static/admin/static/")
            .setCachePeriod(3600)
        logger.info("✅ Registered handler: /admin/static/** -> file:/app/static/admin/static/")
            
        // Also handle without /admin prefix in case of direct requests
        registry.addResourceHandler("/static/js/**", "/static/css/**", "/static/media/**")
            .addResourceLocations("file:/app/static/admin/static/js/", "file:/app/static/admin/static/css/", "file:/app/static/admin/static/media/")
            .setCachePeriod(3600)
        logger.info("✅ Registered fallback handlers for direct static requests")
            
        // Serve admin portal manifest and favicon
        registry.addResourceHandler("/admin/manifest.json", "/admin/favicon.ico", "/admin/logo*.png")
            .addResourceLocations("file:/app/static/admin/")
            .setCachePeriod(3600)
            
        // Serve main app static assets
        registry.addResourceHandler("/static/**")
            .addResourceLocations("file:/app/static/app/static/")
            .setCachePeriod(3600)
            
        // Serve main app root files
        registry.addResourceHandler("/*.js", "/*.css", "/*.ico", "/*.png", "/*.html", "/manifest.json")
            .addResourceLocations("file:/app/static/app/")
            .setCachePeriod(3600)
    }
}