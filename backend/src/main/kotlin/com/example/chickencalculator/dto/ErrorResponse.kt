package com.example.chickencalculator.dto

import java.time.LocalDateTime
import java.util.*

/**
 * Standardized error response DTO for all API errors
 * 
 * @param timestamp When the error occurred
 * @param status HTTP status code
 * @param error Brief error category/type
 * @param message Human-readable error message
 * @param path Request path where the error occurred
 * @param correlationId Unique identifier for tracking this error across logs
 * @param details Additional error details (validation errors, etc.)
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val correlationId: String = UUID.randomUUID().toString(),
    val details: Map<String, Any>? = null
)