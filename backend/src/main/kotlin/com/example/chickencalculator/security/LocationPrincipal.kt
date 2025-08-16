package com.example.chickencalculator.security

import java.security.Principal

/**
 * Principal representing an authenticated location user.
 * Contains location-specific information for authorization decisions.
 */
data class LocationPrincipal(
    val locationId: Long,
    val locationSlug: String,
    val locationName: String
) : Principal {

    override fun getName(): String = locationSlug

    override fun toString(): String {
        return "LocationPrincipal(locationId=$locationId, slug='$locationSlug', name='$locationName')"
    }

    /**
     * Create a LocationPrincipal from JWT claims
     */
    companion object {
        fun fromClaims(
            locationId: Any?,
            locationSlug: String,
            locationName: Any?
        ): LocationPrincipal {
            val id = when (locationId) {
                is Number -> locationId.toLong()
                is String -> locationId.toLongOrNull() ?: throw IllegalArgumentException("Invalid locationId: $locationId")
                else -> throw IllegalArgumentException("LocationId must be a number, got: $locationId")
            }

            val name = when (locationName) {
                is String -> locationName
                null -> locationSlug // fallback to slug if name not available
                else -> locationName.toString()
            }

            return LocationPrincipal(
                locationId = id,
                locationSlug = locationSlug,
                locationName = name
            )
        }
    }
}