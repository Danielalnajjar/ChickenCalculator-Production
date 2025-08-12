package com.example.chickencalculator.exception

/**
 * Exception thrown when authentication credentials are invalid
 */
class InvalidCredentialsException(
    message: String = "Invalid credentials provided",
    val email: String? = null
) : RuntimeException(message) {
    
    constructor(email: String) : this("Invalid credentials for email '$email'", email)
}