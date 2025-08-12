package com.example.chickencalculator.exception

/**
 * Exception thrown when a user lacks required permissions for an operation
 */
class InsufficientPermissionsException(
    message: String = "Insufficient permissions to perform this operation",
    val requiredPermission: String? = null,
    val userRole: String? = null
) : RuntimeException(message) {
    
    constructor(requiredPermission: String, userRole: String) : this(
        "User with role '$userRole' lacks required permission '$requiredPermission'",
        requiredPermission,
        userRole
    )
}