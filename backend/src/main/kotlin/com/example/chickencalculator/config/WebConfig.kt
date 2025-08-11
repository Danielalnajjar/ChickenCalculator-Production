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
        val appStaticDir = File("/app/static/app/static")
        if (appStaticDir.exists()) {
            logger.info("📂 Main app static directory exists at: /app/static/app/static")
            appStaticDir.listFiles()?.forEach { 
                logger.info("   - ${it.name} (${if (it.isDirectory) "directory" else "file"})")
            }
        } else {
            logger.warn("⚠️ Main app static directory not found at: /app/static/app/static")
        }
        
        val adminStaticDir = File("/app/static/admin/static")
        if (adminStaticDir.exists()) {
            logger.info("📂 Admin static directory exists at: /app/static/admin/static")
            adminStaticDir.listFiles()?.forEach { 
                logger.info("   - ${it.name} (${if (it.isDirectory) "directory" else "file"})")
            }
        } else {
            logger.warn("⚠️ Admin static directory not found at: /app/static/admin/static")
        }
        
        // Serve admin portal static assets (CSS, JS, images)
        // This handles requests like /admin/static/js/main.js
        registry.addResourceHandler("/admin/static/**")
            .addResourceLocations("file:/app/static/admin/static/")
            .setCachePeriod(3600)
        logger.info("✅ Registered handler: /admin/static/** -> file:/app/static/admin/static/")
            
        // Serve admin portal manifest and favicon
        registry.addResourceHandler("/admin/manifest.json", "/admin/favicon.ico", "/admin/logo*.png")
            .addResourceLocations("file:/app/static/admin/")
            .setCachePeriod(3600)
        logger.info("✅ Registered handler: /admin/* root files -> file:/app/static/admin/")
            
        // Serve main app static assets
        // Main app files are at /app/static/app/static/ but requests come as /static/
        registry.addResourceHandler("/static/**")
            .addResourceLocations("file:/app/static/app/static/")
            .setCachePeriod(3600)
        logger.info("✅ Registered handler: /static/** -> file:/app/static/app/static/")
            
        // Serve main app root files
        registry.addResourceHandler("/*.js", "/*.css", "/*.ico", "/*.png", "/*.html", "/manifest.json")
            .addResourceLocations("file:/app/static/app/")
            .setCachePeriod(3600)
        logger.info("✅ Registered handler: /* root files -> file:/app/static/app/")
    }
}