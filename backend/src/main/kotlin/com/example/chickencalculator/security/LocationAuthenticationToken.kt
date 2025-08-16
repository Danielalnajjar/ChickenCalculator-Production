package com.example.chickencalculator.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Authentication token for location-based access.
 * Contains location-specific information derived from JWT token validation.
 */
class LocationAuthenticationToken(
    private val principal: LocationPrincipal,
    authorities: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("LOCATION_USER"))
) : AbstractAuthenticationToken(authorities) {

    init {
        super.setAuthenticated(true)
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): LocationPrincipal = principal

    /**
     * Get the location ID from the authenticated principal
     */
    fun getLocationId(): Long = principal.locationId

    /**
     * Get the location slug from the authenticated principal
     */
    fun getLocationSlug(): String = principal.locationSlug

    /**
     * Get the location name from the authenticated principal
     */
    fun getLocationName(): String = principal.locationName

    override fun toString(): String {
        return "LocationAuthenticationToken(locationId=${principal.locationId}, slug='${principal.locationSlug}', name='${principal.locationName}')"
    }
}