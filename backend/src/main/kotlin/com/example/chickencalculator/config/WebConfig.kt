package com.example.chickencalculator.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        println("🔧 Configuring static resource handlers...")
        
        // Serve admin portal static assets (CSS, JS, images)
        registry.addResourceHandler("/admin/static/**")
            .addResourceLocations("file:/app/static/admin/static/")
            .setCachePeriod(3600)
            
        // Serve admin portal favicon
        registry.addResourceHandler("/admin/favicon.ico")
            .addResourceLocations("file:/app/static/admin/")
            .setCachePeriod(3600)
            
        // Serve main app static assets
        registry.addResourceHandler("/static/**")
            .addResourceLocations("file:/app/static/app/static/")
            .setCachePeriod(3600)
            
        // Serve main app root files
        registry.addResourceHandler("/*.js", "/*.css", "/*.ico", "/*.png", "/*.html")
            .addResourceLocations("file:/app/static/app/")
            .setCachePeriod(3600)
    }
}