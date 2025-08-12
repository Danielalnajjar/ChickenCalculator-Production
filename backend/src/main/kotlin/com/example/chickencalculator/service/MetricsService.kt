package com.example.chickencalculator.service

import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * MetricsService provides comprehensive monitoring and observability for the Chicken Calculator application.
 * 
 * This service tracks:
 * - Business metrics: calculations per location, user activity, marination operations
 * - Performance metrics: response times, throughput, database operations
 * - Error tracking: failure rates, exception counts
 * - Multi-tenant metrics: location-specific usage patterns
 * 
 * All metrics are exported in Prometheus format via /actuator/prometheus endpoint.
 */
@Service
class MetricsService @Autowired constructor(
    private val meterRegistry: MeterRegistry
) {
    
    // Business Metrics Counters
    private val calculationCounter = Counter.builder("chicken.calculator.calculations.total")
        .description("Total number of chicken calculations performed")
        .tag("type", "calculation")
        .register(meterRegistry)
        
    private val marinationCounter = Counter.builder("chicken.calculator.marination.total")
        .description("Total number of marination operations")
        .tag("type", "marination")
        .register(meterRegistry)
        
    private val salesDataCounter = Counter.builder("chicken.calculator.sales_data.total")
        .description("Total number of sales data operations")
        .tag("type", "sales")
        .register(meterRegistry)
        
    private val locationAccessCounter = Counter.builder("chicken.calculator.location_access.total")
        .description("Total number of location-specific accesses")
        .tag("type", "location_access")
        .register(meterRegistry)
    
    // Performance Metrics Timers
    private val calculationTimer = Timer.builder("chicken.calculator.calculations.duration")
        .description("Time taken for chicken calculations")
        .tag("operation", "calculate")
        .register(meterRegistry)
        
    private val databaseTimer = Timer.builder("chicken.calculator.database.duration")
        .description("Time taken for database operations")
        .tag("operation", "database")
        .register(meterRegistry)
        
    private val locationLookupTimer = Timer.builder("chicken.calculator.location.lookup.duration")
        .description("Time taken for location lookups")
        .tag("operation", "location_lookup")
        .register(meterRegistry)
    
    // Error Tracking Counters
    private val errorCounter = Counter.builder("chicken.calculator.errors.total")
        .description("Total number of errors")
        .tag("type", "error")
        .register(meterRegistry)
        
    private val authFailureCounter = Counter.builder("chicken.calculator.auth.failures.total")
        .description("Total number of authentication failures")
        .tag("type", "auth_failure")
        .register(meterRegistry)
    
    // Multi-tenant Metrics - Location-specific counters
    private val locationCounters = ConcurrentHashMap<String, Counter>()
    private val locationTimers = ConcurrentHashMap<String, Timer>()
    
    // Gauges for current state
    private val activeLocationsGauge = Gauge.builder("chicken.calculator.locations.active")
        .description("Number of active locations")
        .register(meterRegistry) { getActiveLocationsCount().toDouble() }
        
    private val activeSalesRecordsGauge = Gauge.builder("chicken.calculator.sales.active_records")
        .description("Number of active sales records")
        .register(meterRegistry) { getActiveSalesRecordsCount().toDouble() }
        
    private val activeMarinationRecordsGauge = Gauge.builder("chicken.calculator.marination.active_records")
        .description("Number of active marination records")
        .register(meterRegistry) { getActiveMarinationRecordsCount().toDouble() }
    
    // Internal counters for gauges
    private val activeLocationsCount = AtomicInteger(0)
    private val activeSalesRecordsCount = AtomicLong(0)
    private val activeMarinationRecordsCount = AtomicLong(0)
    
    // Distribution summaries for tracking data sizes
    private val calculationSizeDistribution = DistributionSummary.builder("chicken.calculator.calculation.size")
        .description("Distribution of calculation complexity")
        .tag("metric", "calculation_size")
        .register(meterRegistry)
        
    private val salesDataSizeDistribution = DistributionSummary.builder("chicken.calculator.sales.data_size")
        .description("Distribution of sales data sizes")
        .tag("metric", "sales_data_size")
        .register(meterRegistry)
    
    /**
     * Business Metrics Recording Methods
     */
    
    fun recordCalculation(locationSlug: String? = null, weight: Double = 0.0, processingTimeMs: Long = 0) {
        calculationCounter.increment()
        
        if (locationSlug != null) {
            getLocationCounter(locationSlug, "calculations").increment()
            getLocationTimer(locationSlug, "calculations").record(Duration.ofMillis(processingTimeMs))
        }
        
        if (weight > 0) {
            calculationSizeDistribution.record(weight)
        }
        
        if (processingTimeMs > 0) {
            calculationTimer.record(Duration.ofMillis(processingTimeMs))
        }
    }
    
    fun recordMarinationOperation(locationSlug: String? = null, itemCount: Int = 0) {
        marinationCounter.increment()
        
        if (locationSlug != null) {
            getLocationCounter(locationSlug, "marination").increment()
        }
        
        if (itemCount > 0) {
            activeMarinationRecordsCount.addAndGet(itemCount.toLong())
        }
    }
    
    fun recordSalesDataOperation(locationSlug: String? = null, operation: String = "unknown", dataSize: Int = 0) {
        salesDataCounter.increment(Tags.of("operation", operation))
        
        if (locationSlug != null) {
            getLocationCounter(locationSlug, "sales_data").increment()
        }
        
        if (dataSize > 0) {
            salesDataSizeDistribution.record(dataSize.toDouble())
            
            if (operation == "create") {
                activeSalesRecordsCount.incrementAndGet()
            } else if (operation == "delete") {
                activeSalesRecordsCount.decrementAndGet()
            }
        }
    }
    
    fun recordLocationAccess(locationSlug: String, accessType: String = "view") {
        locationAccessCounter.increment(Tags.of("access_type", accessType, "location", locationSlug))
        getLocationCounter(locationSlug, "access").increment()
    }
    
    fun recordLocationCreated(locationSlug: String) {
        activeLocationsCount.incrementAndGet()
        meterRegistry.counter("chicken.calculator.locations.created.total",
            "location", locationSlug).increment()
    }
    
    fun recordLocationDeleted(locationSlug: String) {
        activeLocationsCount.decrementAndGet()
        meterRegistry.counter("chicken.calculator.locations.deleted.total",
            "location", locationSlug).increment()
    }
    
    /**
     * Performance Metrics Recording Methods
     */
    
    fun recordDatabaseOperation(operation: String, durationMs: Long) {
        databaseTimer.record(Duration.ofMillis(durationMs), Tags.of("operation", operation))
    }
    
    fun recordLocationLookup(locationSlug: String, durationMs: Long, found: Boolean) {
        locationLookupTimer.record(Duration.ofMillis(durationMs), 
            Tags.of("location", locationSlug, "found", found.toString()))
    }
    
    fun <T> timeOperation(operation: String, block: () -> T): T {
        return Timer.Sample.start(meterRegistry).let { sample ->
            try {
                block().also {
                    sample.stop(Timer.builder("chicken.calculator.operation.duration")
                        .tag("operation", operation)
                        .tag("status", "success")
                        .register(meterRegistry))
                }
            } catch (e: Exception) {
                sample.stop(Timer.builder("chicken.calculator.operation.duration")
                    .tag("operation", operation)
                    .tag("status", "error")
                    .register(meterRegistry))
                recordError(operation, e.javaClass.simpleName)
                throw e
            }
        }
    }
    
    /**
     * Error Tracking Methods
     */
    
    fun recordError(operation: String, errorType: String = "unknown", locationSlug: String? = null) {
        errorCounter.increment(Tags.of("operation", operation, "error_type", errorType))
        
        if (locationSlug != null) {
            meterRegistry.counter("chicken.calculator.errors.by_location.total",
                "location", locationSlug, "error_type", errorType).increment()
        }
    }
    
    fun recordAuthFailure(reason: String = "unknown", username: String? = null) {
        authFailureCounter.increment(Tags.of("reason", reason))
        
        if (username != null) {
            meterRegistry.counter("chicken.calculator.auth.failures.by_user.total",
                "username", username, "reason", reason).increment()
        }
    }
    
    /**
     * Admin and Health Metrics
     */
    
    fun recordAdminOperation(operation: String, success: Boolean, durationMs: Long = 0) {
        meterRegistry.counter("chicken.calculator.admin.operations.total",
            "operation", operation, "success", success.toString()).increment()
            
        if (durationMs > 0) {
            meterRegistry.timer("chicken.calculator.admin.operations.duration",
                "operation", operation).record(Duration.ofMillis(durationMs))
        }
    }
    
    fun recordHealthCheck(component: String, healthy: Boolean, responseTimeMs: Long = 0) {
        meterRegistry.counter("chicken.calculator.health.checks.total",
            "component", component, "healthy", healthy.toString()).increment()
            
        if (responseTimeMs > 0) {
            meterRegistry.timer("chicken.calculator.health.checks.duration",
                "component", component).record(Duration.ofMillis(responseTimeMs))
        }
    }
    
    /**
     * Helper Methods for Multi-tenant Metrics
     */
    
    private fun getLocationCounter(locationSlug: String, operation: String): Counter {
        val key = "${locationSlug}_${operation}"
        return locationCounters.computeIfAbsent(key) {
            Counter.builder("chicken.calculator.location.operations.total")
                .description("Operations per location")
                .tag("location", locationSlug)
                .tag("operation", operation)
                .register(meterRegistry)
        }
    }
    
    private fun getLocationTimer(locationSlug: String, operation: String): Timer {
        val key = "${locationSlug}_${operation}"
        return locationTimers.computeIfAbsent(key) {
            Timer.builder("chicken.calculator.location.operations.duration")
                .description("Operation duration per location")
                .tag("location", locationSlug)
                .tag("operation", operation)
                .register(meterRegistry)
        }
    }
    
    /**
     * Gauge Value Providers
     */
    
    private fun getActiveLocationsCount(): Int = activeLocationsCount.get()
    private fun getActiveSalesRecordsCount(): Long = activeSalesRecordsCount.get()
    private fun getActiveMarinationRecordsCount(): Long = activeMarinationRecordsCount.get()
    
    /**
     * Utility Methods for Custom Metrics
     */
    
    fun incrementCustomCounter(name: String, tags: Map<String, String> = emptyMap()) {
        val tagsArray = tags.map { Tag.of(it.key, it.value) }.toTypedArray()
        meterRegistry.counter("chicken.calculator.custom.$name", *tagsArray).increment()
    }
    
    fun recordCustomTimer(name: String, durationMs: Long, tags: Map<String, String> = emptyMap()) {
        val tagsArray = tags.map { Tag.of(it.key, it.value) }.toTypedArray()
        meterRegistry.timer("chicken.calculator.custom.$name", *tagsArray)
            .record(Duration.ofMillis(durationMs))
    }
    
    fun recordCustomGauge(name: String, value: Double, tags: Map<String, String> = emptyMap()) {
        val tagsArray = tags.map { Tag.of(it.key, it.value) }.toTypedArray()
        Gauge.builder("chicken.calculator.custom.$name")
            .tags(*tagsArray)
            .register(meterRegistry) { value }
    }
    
    /**
     * Metrics Summary for Health Checks
     */
    
    fun getMetricsSummary(): Map<String, Any> {
        return mapOf(
            "total_calculations" to calculationCounter.count(),
            "total_marination_operations" to marinationCounter.count(),
            "total_sales_operations" to salesDataCounter.count(),
            "total_location_accesses" to locationAccessCounter.count(),
            "total_errors" to errorCounter.count(),
            "total_auth_failures" to authFailureCounter.count(),
            "active_locations" to getActiveLocationsCount(),
            "active_sales_records" to getActiveSalesRecordsCount(),
            "active_marination_records" to getActiveMarinationRecordsCount(),
            "registered_meters" to meterRegistry.meters.size
        )
    }
}