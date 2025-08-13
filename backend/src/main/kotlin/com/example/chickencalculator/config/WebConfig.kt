package com.example.chickencalculator.config

import com.example.chickencalculator.interceptor.RequestLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Minimal WebConfig - All custom resource handlers removed to fix servlet 500 errors.
 * Spring Boot's default static resource handling will be used instead.
 */
@Configuration
class WebConfig(
    private val requestLoggingInterceptor: RequestLoggingInterceptor
) : WebMvcConfigurer {
    private val logger = LoggerFactory.getLogger(WebConfig::class.java)
    
    override fun addInterceptors(registry: InterceptorRegistry) {
        logger.info("🔧 Configuring request logging interceptor...")
        registry.addInterceptor(requestLoggingInterceptor)
            .addPathPatterns("/**")
            .order(1)
        logger.info("✅ Request logging interceptor registered for all paths")
    }
}