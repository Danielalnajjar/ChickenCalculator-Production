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
        
        // Serve admin portal - MUST come before generic handler
        registry.addResourceHandler("/admin/**")
            .addResourceLocations("classpath:/static/admin/")
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requested = location.createRelative(resourcePath)
                    
                    // Return the exact resource if it exists
                    if (requested.exists() && requested.isReadable) {
                        return requested
                    }
                    
                    // For paths without extensions (React routes), return index.html
                    if (!resourcePath.contains(".")) {
                        val index = location.createRelative("index.html")
                        if (index.exists() && index.isReadable) {
                            return index
                        }
                    }
                    
                    return null
                }
            })
            
        // Serve main app - handles everything else
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/app/", "classpath:/static/")
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    // Skip API and admin paths
                    if (resourcePath.startsWith("api") || resourcePath.startsWith("admin")) {
                        return null
                    }
                    
                    val requested = location.createRelative(resourcePath)
                    
                    // Return the exact resource if it exists
                    if (requested.exists() && requested.isReadable) {
                        return requested
                    }
                    
                    // For paths without extensions (React routes), return index.html
                    if (!resourcePath.contains(".")) {
                        val index = location.createRelative("index.html")
                        if (index.exists() && index.isReadable) {
                            return index
                        }
                    }
                    
                    return null
                }
            })
    }
}