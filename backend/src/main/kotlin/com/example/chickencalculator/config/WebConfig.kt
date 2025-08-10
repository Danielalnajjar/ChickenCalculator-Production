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
            .addResourceLocations("file:/app/static/admin/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    println("📂 Admin resource request: $resourcePath")
                    val resource = location.createRelative(resourcePath)
                    return if (resource.exists() && resource.isReadable) {
                        println("   ✅ Found: ${resource.filename}")
                        resource
                    } else {
                        println("   ↩️ Fallback to index.html")
                        location.createRelative("index.html")
                    }
                }
            })
            
        // Serve main app with React Router support (excluding /api and /admin)
        registry.addResourceHandler("/**")
            .addResourceLocations("file:/app/static/app/")
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