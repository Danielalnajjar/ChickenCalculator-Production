package com.example.chickencalculator.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

@Configuration
class WebConfig : WebMvcConfigurer {
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        println("🔧 Configuring static resource handlers...")
        
        // Serve admin portal with React Router support
        registry.addResourceHandler("/admin/**")
            .addResourceLocations(
                "file:/app/static/admin/",
                "classpath:/static/admin/"
            )
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    println("📂 Admin resource request: $resourcePath")
                    val resource = location.createRelative(resourcePath)
                    
                    // Check if the resource exists
                    if (resource.exists() && resource.isReadable) {
                        println("   ✅ Found: ${resource.filename}")
                        return resource
                    }
                    
                    // For non-file routes (React Router), return index.html
                    // But don't fallback for actual file requests (css, js, etc)
                    if (!resourcePath.contains(".")) {
                        println("   ↩️ Fallback to index.html for React route")
                        return location.createRelative("index.html")
                    }
                    
                    println("   ❌ Resource not found: $resourcePath")
                    return null
                }
            })
            
        // Serve main app with React Router support (excluding /api and /admin)
        registry.addResourceHandler("/**")
            .addResourceLocations(
                "file:/app/static/app/",
                "classpath:/static/app/",
                "classpath:/static/"
            )
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    // Don't handle /api or /admin paths
                    if (resourcePath.startsWith("api") || resourcePath.startsWith("admin")) {
                        return null
                    }
                    val resource = location.createRelative(resourcePath)
                    return if (resource.exists() && resource.isReadable) {
                        resource
                    } else {
                        // Fallback to index.html for React Router
                        location.createRelative("index.html")
                    }
                }
            })
    }
}