package com.example.chickencalculator.config

import org.springframework.context.annotation.Configuration

/**
 * Configuration for API versioning
 * Defines constants for API version management
 */
@Configuration
class ApiVersionConfig {
    companion object {
        /**
         * Current API version prefix
         * Used across all controllers to maintain consistency
         */
        const val API_VERSION = "/api/v1"
        
        /**
         * Legacy API prefix for backward compatibility
         */
        const val API_LEGACY = "/api"
        
        /**
         * Health check endpoint (unversioned)
         */
        const val HEALTH_ENDPOINT = "/api/health"
    }
}