package com.example.chickencalculator.config

import io.sentry.spring.jakarta.EnableSentry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.sentry.SentryOptions
import io.sentry.spring.jakarta.SentryUserProvider
import org.springframework.security.core.context.SecurityContextHolder
import io.sentry.Sentry
import jakarta.annotation.PostConstruct

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
     * Initialize Sentry configuration after bean creation.
     * This replaces the previous @Bean approach which was incorrect for Sentry 7.0.0
     */
    @PostConstruct
    fun initializeSentry() {
        if (sentryDsn.isNotBlank()) {
            Sentry.init { options ->
                options.dsn = sentryDsn
                options.environment = environment
                options.release = "$applicationName@1.0.0"
                
                // Configure sampling rates
                options.tracesSampleRate = when (environment) {
                    "production" -> 0.1 // 10% sampling in production
                    "staging" -> 0.5    // 50% sampling in staging
                    else -> 1.0         // 100% sampling in development
                }
                
                // Enable performance monitoring
                options.enableTracing = true
                
                // Configure which exceptions to capture
                options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
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
     * Note: SentryExceptionResolver is automatically configured by @EnableSentry in Sentry 7.0.0
     * No manual configuration needed - the annotation handles this
     */
}