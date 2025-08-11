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
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    println("📂 Admin resource request: $resourcePath")
                    
                    // Try to find the exact resource
                    val resource = location.createRelative(resourcePath)
                    if (resource.exists() && resource.isReadable) {
                        println("   ✅ Found: ${resource.filename}")
                        return resource
                    }
                    
                    // For routes without file extensions (React Router paths), return index.html
                    // But skip this for actual file requests (css, js, images, etc)
                    val hasExtension = resourcePath.contains(".")
                    if (!hasExtension) {
                        println("   ↩️ Fallback to index.html for React route: $resourcePath")
                        val indexResource = location.createRelative("index.html")
                        if (indexResource.exists() && indexResource.isReadable) {
                            return indexResource
                        }
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