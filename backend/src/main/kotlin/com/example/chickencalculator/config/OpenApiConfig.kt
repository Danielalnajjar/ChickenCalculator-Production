package com.example.chickencalculator.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    
    @Value("\${server.port:8080}")
    private val serverPort: String = "8080"
    
    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearer-jwt"
        
        return OpenAPI()
            .info(apiInfo())
            .addServersItem(Server()
                .url("http://localhost:$serverPort")
                .description("Local Development Server"))
            .addServersItem(Server()
                .url("https://chicken-calculator.railway.app")
                .description("Production Server"))
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Authentication - Enter the token without 'Bearer' prefix")
                    )
            )
    }
    
    private fun apiInfo(): Info {
        return Info()
            .title("Chicken Calculator API")
            .description("""
                |## Overview
                |The Chicken Calculator API provides endpoints for managing chicken marination calculations, 
                |sales data tracking, and multi-location restaurant management.
                |
                |## Authentication
                |Most endpoints require JWT authentication. Use the `/api/admin/auth/login` endpoint to obtain a token.
                |
                |## Main Features
                |- **Calculator**: Calculate raw chicken requirements and marination distribution
                |- **Sales Management**: Track daily sales data by location
                |- **Location Management**: Manage multiple restaurant locations
                |- **Marination History**: Track marination logs and history
                |- **Admin Portal**: Administrative functions for system management
                |
                |## Rate Limiting
                |API requests are limited to 100 requests per minute per IP address.
                |
                |## Support
                |For API support, please contact the development team.
            """.trimMargin())
            .version("1.0.0")
            .contact(
                Contact()
                    .name("Chicken Calculator Support")
                    .email("support@yourcompany.com")
                    .url("https://yourcompany.com/support")
            )
            .license(
                License()
                    .name("Proprietary")
                    .url("https://yourcompany.com/license")
            )
    }
}