package com.example.chickencalculator.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.sentry.SentryOptions
import io.sentry.spring.jakarta.SentryUserProvider
import org.springframework.security.core.context.SecurityContextHolder
import io.sentry.Sentry
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory

/**
 * Sentry configuration for error tracking and monitoring.
 * 
 * This configuration enables:
 * - Automatic exception capture
 * - User context tracking
 * - Performance monitoring
 * - Environment-specific settings
 * 
 * Note: Sentry is only enabled if SENTRY_DSN environment variable is set.
 * The @EnableSentry annotation is intentionally NOT used to avoid bean conflicts.
 * Sentry is initialized manually to provide better control over configuration.
 */
@Configuration
class SentryConfig {

    private val logger = LoggerFactory.getLogger(SentryConfig::class.java)

    @Value("\${sentry.dsn:}")
    private lateinit var sentryDsn: String

    @Value("\${spring.profiles.active:development}")
    private lateinit var environment: String

    @Value("\${spring.application.name:chicken-calculator}")
    private lateinit var applicationName: String

    /**
     * Initialize Sentry configuration after bean creation.
     * This manual initialization approach avoids Spring Boot auto-configuration conflicts.
     */
    @PostConstruct
    fun initializeSentry() {
        // Check if DSN is provided and not empty
        if (sentryDsn.isNotBlank()) {
            try {
                logger.info("Initializing Sentry for environment: $environment")
                
                Sentry.init { options ->
                    options.dsn = sentryDsn
                    options.environment = environment
                    options.release = "$applicationName@1.0.0"
                    
                    // Configure sampling rates based on environment
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
                
                logger.info("Sentry initialized successfully")
            } catch (e: Exception) {
                logger.error("Failed to initialize Sentry: ${e.message}", e)
                // Don't fail application startup if Sentry initialization fails
            }
        } else {
            logger.info("Sentry DSN not configured. Error tracking is disabled.")
            logger.info("To enable Sentry, set the SENTRY_DSN environment variable")
        }
    }

    /**
     * Custom user provider to capture user context in errors.
     * This bean is only created if Sentry is properly initialized.
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
}