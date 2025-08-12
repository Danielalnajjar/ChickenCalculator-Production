package com.example.chickencalculator.exception

/**
 * Exception thrown when a requested location is not found
 */
class LocationNotFoundException(
    message: String = "Location not found",
    val locationId: Long? = null,
    val slug: String? = null
) : RuntimeException(message) {
    
    constructor(locationId: Long) : this("Location with ID '$locationId' not found", locationId)
    constructor(slug: String, isSlug: Boolean) : this("Location with slug '$slug' not found", null, slug)
}