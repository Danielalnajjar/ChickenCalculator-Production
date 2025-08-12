package com.example.chickencalculator.exception

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GlobalExceptionHandlerTest {

    private val globalExceptionHandler = GlobalExceptionHandler()

    @Test
    fun `should handle LocationNotFoundException with correlation ID`() {
        val exception = LocationNotFoundException(123L)
        val request = createMockWebRequest("/api/locations/123")
        
        val response = globalExceptionHandler.handleLocationNotFoundException(exception, request)
        
        assertEquals(404, response.statusCode.value())
        assertNotNull(response.body?.correlationId)
        assertEquals("Location Not Found", response.body?.error)
        assertEquals("/api/locations/123", response.body?.path)
    }

    @Test
    fun `should handle InvalidCredentialsException with correlation ID`() {
        val exception = InvalidCredentialsException("test@example.com")
        val request = createMockWebRequest("/api/admin/auth/login")
        
        val response = globalExceptionHandler.handleInvalidCredentialsException(exception, request)
        
        assertEquals(401, response.statusCode.value())
        assertNotNull(response.body?.correlationId)
        assertEquals("Authentication Failed", response.body?.error)
        assertEquals("/api/admin/auth/login", response.body?.path)
    }

    @Test
    fun `should handle BusinessValidationException with field errors`() {
        val fieldErrors = mapOf("name" to "Name is required", "email" to "Invalid email format")
        val exception = BusinessValidationException(fieldErrors)
        val request = createMockWebRequest("/api/admin/locations")
        
        val response = globalExceptionHandler.handleBusinessValidationException(exception, request)
        
        assertEquals(400, response.statusCode.value())
        assertNotNull(response.body?.correlationId)
        assertEquals("Business Validation Failed", response.body?.error)
        assertEquals("/api/admin/locations", response.body?.path)
        assertEquals(fieldErrors, response.body?.details?.get("fieldErrors"))
    }

    @Test
    fun `should handle InsufficientPermissionsException`() {
        val exception = InsufficientPermissionsException("ADMIN", "USER")
        val request = createMockWebRequest("/api/admin/locations")
        
        val response = globalExceptionHandler.handleInsufficientPermissionsException(exception, request)
        
        assertEquals(403, response.statusCode.value())
        assertNotNull(response.body?.correlationId)
        assertEquals("Access Denied", response.body?.error)
        assertEquals("/api/admin/locations", response.body?.path)
    }

    @Test
    fun `should handle generic Exception with appropriate message`() {
        val exception = RuntimeException("Test error")
        val request = createMockWebRequest("/api/test")
        
        val response = globalExceptionHandler.handleGenericException(exception, request)
        
        assertEquals(500, response.statusCode.value())
        assertNotNull(response.body?.correlationId)
        assertEquals("Internal Server Error", response.body?.error)
        assertEquals("/api/test", response.body?.path)
        // In non-production, should show actual error message
        assertEquals("Test error", response.body?.message)
    }

    private fun createMockWebRequest(uri: String): ServletWebRequest {
        val mockRequest = MockHttpServletRequest().apply {
            requestURI = uri
        }
        return ServletWebRequest(mockRequest)
    }
}