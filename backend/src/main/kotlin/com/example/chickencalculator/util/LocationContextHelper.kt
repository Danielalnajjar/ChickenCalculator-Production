package com.example.chickencalculator.util

import com.example.chickencalculator.security.LocationAuthenticationToken
import com.example.chickencalculator.security.LocationPrincipal
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Utility class for extracting location context from Spring Security context.
 * Provides a centralized way to get current tenant information from JWT-based authentication.
 */
object LocationContextHelper {

    /**
     * Get the current location ID from the security context.
     * Returns null if no location authentication is present.
     */
    fun getCurrentLocationId(): Long? {
        return getCurrentLocationAuthentication()?.getLocationId()
    }

    /**
     * Get the current location slug from the security context.
     * Returns null if no location authentication is present.
     */
    fun getCurrentLocationSlug(): String? {
        return getCurrentLocationAuthentication()?.getLocationSlug()
    }

    /**
     * Get the current location name from the security context.
     * Returns null if no location authentication is present.
     */
    fun getCurrentLocationName(): String? {
        return getCurrentLocationAuthentication()?.getLocationName()
    }

    /**
     * Get the current location principal from the security context.
     * Returns null if no location authentication is present.
     */
    fun getCurrentLocationPrincipal(): LocationPrincipal? {
        return getCurrentLocationAuthentication()?.principal
    }

    /**
     * Get the current location authentication token from the security context.
     * Returns null if no location authentication is present.
     */
    fun getCurrentLocationAuthentication(): LocationAuthenticationToken? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication is LocationAuthenticationToken) {
            authentication
        } else {
            null
        }
    }

    /**
     * Check if the current user is authenticated for a specific location.
     */
    fun isAuthenticatedForLocation(locationSlug: String): Boolean {
        val currentSlug = getCurrentLocationSlug()
        return currentSlug != null && currentSlug == locationSlug
    }

    /**
     * Check if the current user is authenticated for a specific location ID.
     */
    fun isAuthenticatedForLocation(locationId: Long): Boolean {
        val currentLocationId = getCurrentLocationId()
        return currentLocationId != null && currentLocationId == locationId
    }

    /**
     * Get current location context or throw exception if not authenticated.
     * Use this when location authentication is required.
     */
    fun requireLocationContext(): LocationPrincipal {
        return getCurrentLocationPrincipal()
            ?: throw IllegalStateException("No location authentication found in security context")
    }

    /**
     * Clear location context (useful for testing)
     */
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }
}