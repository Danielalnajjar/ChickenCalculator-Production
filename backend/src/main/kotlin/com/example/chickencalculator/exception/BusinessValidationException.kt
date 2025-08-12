package com.example.chickencalculator.exception

/**
 * Exception thrown when business logic validation fails
 */
class BusinessValidationException(
    message: String,
    val fieldErrors: Map<String, String>? = null,
    val errorCode: String? = null
) : RuntimeException(message) {
    
    companion object {
        // Factory method for single field validation error
        fun forField(fieldName: String, fieldError: String): BusinessValidationException {
            return BusinessValidationException(
                message = "Validation failed for field '$fieldName': $fieldError",
                fieldErrors = mapOf(fieldName to fieldError)
            )
        }
        
        // Factory method for multiple field validation errors
        fun forFields(fieldErrors: Map<String, String>): BusinessValidationException {
            return BusinessValidationException(
                message = "Multiple validation errors occurred",
                fieldErrors = fieldErrors
            )
        }
        
        // Factory method with error code for specific business rules
        fun withErrorCode(message: String, errorCode: String): BusinessValidationException {
            return BusinessValidationException(
                message = message,
                fieldErrors = null,
                errorCode = errorCode
            )
        }
    }
}