package com.example.chickencalculator.config

import io.sentry.spring.jakarta.EnableSentry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.sentry.SentryOptions
import io.sentry.spring.jakarta.SentryExceptionResolver
import io.sentry.spring.jakarta.SentryUserProvider
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Sentry configuration for error tracking and monitoring.
 * 
 * This configuration enables:
 * - Automatic exception capture
 * - User context tracking
 * - Performance monitoring
 * - Environment-specific settings
 */
@Configuration
@EnableSentry
class SentryConfig {

    @Value("\${sentry.dsn:}")
    private lateinit var sentryDsn: String

    @Value("\${spring.profiles.active:development}")
    private lateinit var environment: String

    @Value("\${spring.application.name:chicken-calculator}")
    private lateinit var applicationName: String

    /**
     * Configure Sentry options for the application.
     */
    @Bean
    fun sentryOptions(): SentryOptions.OptionsConfiguration<SentryOptions> {
        return SentryOptions.OptionsConfiguration { options ->
            // Set environment and release information
            options.environment = environment
            options.release = "$applicationName@1.0.0"
            
            // Configure sampling rates
            options.tracesSampleRate = when (environment) {
                "production" -> 0.1 // 10% sampling in production
                "staging" -> 0.5    // 50% sampling in staging
                else -> 1.0         // 100% sampling in development
            }
            
            // Enable performance monitoring
            options.isEnableTracing = true
            
            // Configure which exceptions to capture
            options.setBeforeSend { event, _ ->
                // Filter out health check related exceptions
                if (event.transaction?.contains("/actuator/health") == true) {
                    null
                } else {
                    event
                }
            }
            
            // Add custom tags
            options.setTag("service", "chicken-calculator")
            options.setTag("component", "backend")
            
            // Enable debug mode in development
            options.isDebug = environment == "development"
        }
    }

    /**
     * Custom user provider to capture user context in errors.
     */
    @Bean
    fun sentryUserProvider(): SentryUserProvider {
        return SentryUserProvider {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication?.isAuthenticated == true && authentication.name != "anonymousUser") {
                io.sentry.protocol.User().apply {
                    username = authentication.name
                    // Don't include sensitive information like email in production
                    if (environment != "production") {
                        email = authentication.name
                    }
                }
            } else {
                null
            }
        }
    }

    /**
     * Custom exception resolver for additional error context.
     */
    @Bean
    fun sentryExceptionResolver(): HandlerExceptionResolver {
        return SentryExceptionResolver()
    }
}