package com.example.chickencalculator.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Serve admin portal static files
        registry.addResourceHandler("/admin/**")
            .addResourceLocations("file:/app/static/admin/")
            
        // Serve main app static files
        registry.addResourceHandler("/**")
            .addResourceLocations("file:/app/static/app/")
            .resourceChain(true)
    }
    
    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Handle React Router for admin portal
        registry.addViewController("/admin/{path:[^\\.]*}")
            .setViewName("forward:/admin/index.html")
            
        // Handle React Router for main app
        registry.addViewController("/{path:[^\\.]*}")
            .setViewName("forward:/index.html")
    }
}