package com.example.chickencalculator.config

import io.sentry.Sentry
import io.sentry.SentryOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.net.URI

/**
 * Sentry configuration for error tracking and monitoring.
 * 
 * This configuration enables:
 * - Automatic exception capture with filtering
 * - User context tracking (without PII)
 * - Performance monitoring with sampling
 * - Multi-tenant location context
 * 
 * The configuration is resilient and will not cause application failures.
 * When SENTRY_DSN is empty, Sentry operates in no-op mode.
 */
@Configuration
class SentryConfig {
    
    private val logger = LoggerFactory.getLogger(SentryConfig::class.java)
    
    @Value("\${sentry.dsn:}")
    private lateinit var sentryDsn: String
    
    @Value("\${sentry.environment:production}")
    private lateinit var environment: String
    
    @Value("\${sentry.release:unknown}")
    private lateinit var release: String
    
    @Value("\${sentry.traces-sample-rate:0.10}")
    private var tracesSampleRate: Double = 0.10
    
    @Value("\${sentry.profiles-sample-rate:0.01}")
    private var profilesSampleRate: Double = 0.01
    
    @PostConstruct
    fun initializeSentry() {
        // Only initialize if DSN is provided
        if (sentryDsn.isBlank()) {
            logger.info("Sentry DSN not configured. Error tracking is disabled.")
            return
        }
        
        try {
            logger.info("Initializing Sentry for environment: $environment")
            
            Sentry.init { options ->
                options.dsn = sentryDsn
                options.environment = environment
                options.release = release
                
                // Never send PII by default
                options.isSendDefaultPii = false
                
                // Set reasonable timeouts to prevent hanging
                options.connectionTimeoutMillis = 5_000
                options.readTimeoutMillis = 5_000
                
                // Configure sampling rates
                options.tracesSampleRate = tracesSampleRate
                options.profilesSampleRate = profilesSampleRate
                
                // Enable performance monitoring
                options.enableTracing = true
                
                // Filter out noise and add context
                options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    try {
                        val url = event.request?.url.orEmpty()
                        val path = runCatching { URI(url).path ?: url }.getOrElse { url }
                        
                        // Drop health/static/debug noise
                        if (path.startsWith("/api/health") ||
                            path.startsWith("/actuator") ||
                            path.startsWith("/probe") ||      // Debug endpoints (dev profile only)
                            path.startsWith("/assets") ||
                            path.startsWith("/static") ||
                            path == "/favicon.ico") {
                            return@BeforeSendCallback null
                        }
                        
                        // Add lightweight multi-tenant context (non-PII)
                        val slugHeader = event.request?.headers?.get("X-Location-Id")
                        if (!slugHeader.isNullOrBlank()) {
                            event.contexts["location"] = mapOf("slug" to slugHeader)
                        }
                        
                        event
                    } catch (e: Exception) {
                        // Never let telemetry break the app
                        logger.debug("Sentry beforeSend filter error", e)
                        null
                    }
                }
                
                // Dynamic sampling for traces
                options.tracesSampler = SentryOptions.TracesSamplerCallback { samplingContext ->
                    val url = samplingContext.customSamplingContext?.get("url") as? String ?: ""
                    if (url.startsWith("/api/health") || url.startsWith("/actuator")) {
                        0.0
                    } else {
                        System.getenv("SENTRY_TRACES_SAMPLE_RATE")?.toDoubleOrNull() ?: tracesSampleRate
                    }
                }
            }
            
            logger.info("Sentry initialized successfully for environment: $environment")
        } catch (e: Exception) {
            logger.error("Failed to initialize Sentry: ${e.message}", e)
            // Don't fail application startup if Sentry initialization fails
        }
    }
}