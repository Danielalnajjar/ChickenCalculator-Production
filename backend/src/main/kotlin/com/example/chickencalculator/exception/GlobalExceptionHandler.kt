package com.example.chickencalculator.exception

import com.example.chickencalculator.dto.ErrorResponse
import com.example.chickencalculator.filter.CorrelationIdContext
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.*

/**
 * Global exception handler that provides standardized error responses
 * across the entire application with correlation IDs and proper logging.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    companion object {
        private const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        private const val GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later."
    }
    
    // ===== Custom Business Exceptions =====
    
    @ExceptionHandler(LocationNotFoundException::class)
    fun handleLocationNotFoundException(
        ex: LocationNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Location not found", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.NOT_FOUND,
            error = "Location Not Found",
            message = ex.message ?: "The requested location was not found",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentialsException(
        ex: InvalidCredentialsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Invalid credentials", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "Authentication Failed",
            message = ex.message ?: "Invalid email or password",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(InsufficientPermissionsException::class)
    fun handleInsufficientPermissionsException(
        ex: InsufficientPermissionsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Insufficient permissions", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.FORBIDDEN,
            error = "Access Denied",
            message = ex.message ?: "You do not have permission to access this resource",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(BusinessValidationException::class)
    fun handleBusinessValidationException(
        ex: BusinessValidationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Business validation failed", ex, correlationId, "WARN")
        
        val details = ex.fieldErrors?.takeIf { it.isNotEmpty() }?.let { errors ->
            mapOf("fieldErrors" to errors)
        }
        
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "Business Validation Failed",
            message = ex.message ?: "The request contains invalid data",
            path = getPath(request),
            correlationId = correlationId,
            details = details
        )
    }
    
    // ===== Spring Validation Exceptions =====
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        
        val fieldErrors = ex.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }
        
        val globalErrors = ex.bindingResult.globalErrors.associate { error ->
            error.objectName to (error.defaultMessage ?: "Validation failed")
        }
        
        val allErrors = fieldErrors + globalErrors
        val errorMessage = allErrors.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        
        logException("Validation failed: $errorMessage", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "Validation Failed",
            message = "Request validation failed",
            path = getPath(request),
            correlationId = correlationId,
            details = mapOf("fieldErrors" to allErrors)
        )
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        
        val violations = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to (violation.message ?: "Constraint violation")
        }
        
        val errorMessage = violations.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        logException("Constraint violation: $errorMessage", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "Constraint Violation",
            message = "Request constraints were violated",
            path = getPath(request),
            correlationId = correlationId,
            details = mapOf("violations" to violations)
        )
    }
    
    // ===== Spring Security Exceptions =====
    
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(
        ex: BadCredentialsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Authentication failed", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "Authentication Failed",
            message = "Invalid email or password",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Authentication error", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "Authentication Required",
            message = "Please authenticate to access this resource",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Access denied", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.FORBIDDEN,
            error = "Access Denied",
            message = "You do not have permission to access this resource",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    // ===== JWT Exceptions =====
    
    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwtException(
        ex: ExpiredJwtException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("JWT token expired", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "Token Expired",
            message = "Your session has expired. Please login again",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(MalformedJwtException::class, SignatureException::class)
    fun handleInvalidJwtException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Invalid JWT token", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            error = "Invalid Token",
            message = "Invalid authentication token",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    // ===== Common Spring Web Exceptions =====
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(
        ex: HttpRequestMethodNotSupportedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        val supportedMethods = ex.supportedMethods?.joinToString(", ") ?: "Unknown"
        logException("Method not supported: ${ex.method}. Supported: $supportedMethods", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            error = "Method Not Allowed",
            message = "HTTP method '${ex.method}' is not supported for this endpoint",
            path = getPath(request),
            correlationId = correlationId,
            details = mapOf("supportedMethods" to supportedMethods)
        )
    }
    
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupportedException(
        ex: HttpMediaTypeNotSupportedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        val supportedTypes = ex.supportedMediaTypes.joinToString(", ")
        logException("Media type not supported: ${ex.contentType}. Supported: $supportedTypes", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            error = "Unsupported Media Type",
            message = "Content type '${ex.contentType}' is not supported",
            path = getPath(request),
            correlationId = correlationId,
            details = mapOf("supportedMediaTypes" to supportedTypes)
        )
    }
    
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Message not readable", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "Malformed Request",
            message = "Request body could not be read or parsed",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameterException(
        ex: MissingServletRequestParameterException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Missing request parameter: ${ex.parameterName}", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "Missing Parameter",
            message = "Required parameter '${ex.parameterName}' is missing",
            path = getPath(request),
            correlationId = correlationId,
            details = mapOf("parameter" to ex.parameterName, "type" to ex.parameterType)
        )
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        val expectedType = ex.requiredType?.simpleName ?: "Unknown"
        logException("Type mismatch for parameter: ${ex.name}", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "Type Mismatch",
            message = "Parameter '${ex.name}' should be of type $expectedType",
            path = getPath(request),
            correlationId = correlationId,
            details = mapOf(
                "parameter" to ex.name,
                "expectedType" to expectedType,
                "actualValue" to (ex.value?.toString() ?: "null")
            )
        )
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Invalid argument", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            error = "Invalid Request",
            message = ex.message ?: "Invalid request parameters",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Illegal state", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.CONFLICT,
            error = "Invalid State",
            message = ex.message ?: "The requested operation cannot be performed in the current state",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFoundException(
        ex: NoSuchElementException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Resource not found", ex, correlationId, "WARN")
        
        return buildErrorResponse(
            status = HttpStatus.NOT_FOUND,
            error = "Not Found",
            message = ex.message ?: "Requested resource not found",
            path = getPath(request),
            correlationId = correlationId
        )
    }
    
    // ===== Generic Exception Handler =====
    
    // Temporarily disabled to debug servlet exceptions
    // @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = getCorrelationId()
        logException("Unexpected error occurred", ex, correlationId, "ERROR")
        
        // Don't expose internal error details in production
        val message = if (isProduction()) {
            GENERIC_ERROR_MESSAGE
        } else {
            ex.message ?: "Unknown error"
        }
        
        return buildErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            error = "Internal Server Error",
            message = message,
            path = getPath(request),
            correlationId = correlationId,
            details = if (!isProduction()) mapOf("exceptionType" to (ex::class.simpleName ?: "Unknown")) else null
        )
    }
    
    // ===== Utility Methods =====
    
    private fun getCorrelationId(): String {
        // Get correlation ID from MDC (set by CorrelationIdFilter)
        // If for some reason it's not present, generate one as fallback
        return CorrelationIdContext.getOrGenerateCorrelationId()
    }
    
    private fun buildErrorResponse(
        status: HttpStatus,
        error: String,
        message: String,
        path: String,
        correlationId: String,
        details: Map<String, Any>? = null
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = status.value(),
            error = error,
            message = message,
            path = path,
            correlationId = correlationId,
            details = details
        )
        
        return ResponseEntity.status(status)
            .header(CORRELATION_ID_HEADER, correlationId)
            .body(errorResponse)
    }
    
    private fun getPath(request: WebRequest): String {
        return try {
            val servletRequest = (request as? org.springframework.web.context.request.ServletWebRequest)?.request as? HttpServletRequest
            servletRequest?.requestURI ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun logException(message: String, ex: Throwable, correlationId: String, level: String) {
        val logMessage = "[$correlationId] $message: ${ex.message}"
        
        when (level.uppercase()) {
            "ERROR" -> logger.error(logMessage, ex)
            "WARN" -> logger.warn(logMessage)
            "INFO" -> logger.info(logMessage)
            "DEBUG" -> logger.debug(logMessage)
            else -> logger.warn(logMessage)
        }
    }
    
    private fun isProduction(): Boolean {
        val profile = System.getenv("SPRING_PROFILES_ACTIVE")
        return profile == "production" || profile == "prod"
    }
}