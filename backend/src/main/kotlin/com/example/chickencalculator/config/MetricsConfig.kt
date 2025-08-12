package com.example.chickencalculator.config

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * MetricsConfig configures Micrometer metrics collection and processing for the Chicken Calculator application.
 * 
 * This configuration:
 * - Enables @Timed annotation processing through TimedAspect
 * - Customizes meter registry with common tags and filters
 * - Configures metric naming and distribution summaries
 * - Sets up performance tracking for business operations
 */
@Configuration
class MetricsConfig {
    
    /**
     * Enable @Timed annotation processing.
     * This allows controllers and services to use @Timed annotations for automatic metrics collection.
     */
    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect {
        return TimedAspect(registry)
    }
    
    /**
     * Customize the meter registry with common tags and filters.
     * This adds application-wide tags and configures metric filtering.
     */
    @Bean
    fun meterRegistryCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            // Add common tags to all metrics
            registry.config()
                .commonTags(listOf(
                    Tag.of("application", "chicken-calculator"),
                    Tag.of("component", "backend"),
                    Tag.of("version", "1.0.0")
                ))
                // Configure meter filters
                .meterFilter(MeterFilter.deny { id ->
                    // Filter out noisy JVM metrics that aren't useful for business monitoring
                    val name = id.name
                    name.startsWith("jvm.gc.overhead") ||
                    name.startsWith("jvm.gc.concurrent.phase.time") ||
                    name.startsWith("jvm.compilation.time")
                })
                .meterFilter(MeterFilter.denyNameStartsWith("tomcat.threads"))
                .meterFilter(MeterFilter.denyNameStartsWith("logback"))
                // Rename some metrics for better clarity
                .meterFilter(MeterFilter.renameTag("chicken.calculator.calculations.total", "type", "operation"))
        }
    }
    
    /**
     * Configure custom tags for environment-specific metrics.
     * This allows different environments to be distinguished in monitoring dashboards.
     */
    @Bean
    fun environmentMeterFilter(): MeterFilter {
        val environment = System.getenv("SPRING_PROFILES_ACTIVE") ?: "development"
        val railwayEnvironment = System.getenv("RAILWAY_ENVIRONMENT") ?: "unknown"
        
        return MeterFilter.commonTags(listOf(
            Tag.of("environment", environment),
            Tag.of("deployment.platform", "railway"),
            Tag.of("deployment.environment", railwayEnvironment)
        ))
    }
}