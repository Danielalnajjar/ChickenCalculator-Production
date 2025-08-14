package com.example.chickencalculator.config

import com.example.chickencalculator.interceptor.RequestLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit

/**
 * WebConfig - Handles static resource mapping and request interceptors
 */
@Configuration
class WebConfig(
    private val requestLoggingInterceptor: RequestLoggingInterceptor
) : WebMvcConfigurer {
    private val logger = LoggerFactory.getLogger(WebConfig::class.java)
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Admin portal static assets - the URL pattern /admin/static/** maps to files in /app/static/admin/static/**
        registry.addResourceHandler("/admin/static/**")
            .addResourceLocations("file:/app/static/admin/static/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .resourceChain(true)
        
        // Admin portal root files (favicon, manifest, etc)
        registry.addResourceHandler("/admin/**")
            .addResourceLocations("file:/app/static/admin/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .resourceChain(true)
        
        // Location app static assets
        registry.addResourceHandler("/location/*/static/**")
            .addResourceLocations("file:/app/static/app/static/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .resourceChain(true)
            
        logger.info("✅ Static resource handlers configured for admin and location apps")
    }
    
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(requestLoggingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/admin/static/**", 
                "/admin/favicon.ico",
                "/admin/manifest.json",
                "/location/*/static/**", 
                "/static/**", 
                "/assets/**"
            )
            .order(1)
        logger.info("✅ Request logging interceptor registered with static resource exclusions")
    }
}