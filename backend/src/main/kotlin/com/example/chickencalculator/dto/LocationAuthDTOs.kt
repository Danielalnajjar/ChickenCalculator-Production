package com.example.chickencalculator.dto

import jakarta.validation.constraints.NotBlank

/**
 * Request for location authentication
 */
data class LocationAuthRequest(
    @field:NotBlank(message = "Password is required")
    val password: String
)

/**
 * Response for successful location authentication
 */
data class LocationAuthResponse(
    val success: Boolean,
    val message: String,
    val slug: String,
    val expiresIn: Int // seconds until expiry
)