package com.example.chickencalculator.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
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
            appStaticDir.listFiles()?.take(5)?.forEach { 
                logger.info("   - ${it.name} (${if (it.isDirectory) "directory" else "file"})")
            }
        } else {
            logger.warn("⚠️ Main app static directory not found at: /app/static/app/static")
        }
        
        val adminStaticDir = File("/app/static/admin/static")
        if (adminStaticDir.exists()) {
            logger.info("📂 Admin static directory exists at: /app/static/admin/static")
            adminStaticDir.listFiles()?.take(5)?.forEach { 
                logger.info("   - ${it.name} (${if (it.isDirectory) "directory" else "file"})")
            }
        } else {
            logger.warn("⚠️ Admin static directory not found at: /app/static/admin/static")
        }
        
        // IMPORTANT: Static resources should be handled with high priority
        // Main app static assets (JS, CSS, images)
        registry.addResourceHandler("/static/**")
            .addResourceLocations(
                "file:/app/static/app/static/",
                "classpath:/static/app/static/",
                "classpath:/static/"
            )
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(PathResourceResolver())
        logger.info("✅ Registered handler: /static/** -> /app/static/app/static/")
        
        // Admin portal static assets
        registry.addResourceHandler("/admin/static/**")
            .addResourceLocations(
                "file:/app/static/admin/static/",
                "classpath:/static/admin/static/",
                "classpath:/admin/static/"
            )
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(PathResourceResolver())
        logger.info("✅ Registered handler: /admin/static/** -> /app/static/admin/static/")
        
        // Main app root files (favicon, manifest, etc.)
        registry.addResourceHandler("/favicon.ico", "/manifest.json", "/robots.txt", "/logo*.png")
            .addResourceLocations(
                "file:/app/static/app/",
                "classpath:/static/app/",
                "classpath:/static/"
            )
            .setCachePeriod(86400)
        logger.info("✅ Registered handler: root files -> /app/static/app/")
        
        // Admin portal root files
        registry.addResourceHandler("/admin/favicon.ico", "/admin/manifest.json", "/admin/logo*.png")
            .addResourceLocations(
                "file:/app/static/admin/",
                "classpath:/static/admin/"
            )
            .setCachePeriod(86400)
        logger.info("✅ Registered handler: /admin/ root files -> /app/static/admin/")
        
        // Fallback for any missed static resources
        registry.addResourceHandler("/resources/**")
            .addResourceLocations(
                "file:/app/static/",
                "classpath:/static/",
                "classpath:/public/",
                "classpath:/resources/"
            )
            .setCachePeriod(3600)
        logger.info("✅ Registered fallback handler: /resources/**")
    }
    
    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Set order to ensure these are processed after resource handlers
        registry.setOrder(Ordered.LOWEST_PRECEDENCE)
    }
}