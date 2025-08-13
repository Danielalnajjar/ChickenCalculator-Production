package com.example.chickencalculator.controller

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Custom error controller to handle all error cases in the application.
 * Implements Spring Boot's ErrorController interface to provide custom error pages
 * instead of the default Whitelabel error page.
 */
@Controller
class CustomErrorController : ErrorController {
    
    private val logger = LoggerFactory.getLogger(CustomErrorController::class.java)
    
    /**
     * Handle all errors that reach the /error endpoint.
     * This includes 404s, 500s, and any other unhandled errors.
     */
    @RequestMapping("/error")
    @ResponseBody
    fun handleError(request: HttpServletRequest): ResponseEntity<String> {
        // Get the error status code
        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as? Int ?: 500
        val errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE) as? String
        val errorUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) as? String
        
        logger.warn("🚨 Error handler invoked - Status: $status, URI: $errorUri, Message: $errorMessage")
        
        // Generate appropriate error page based on status code
        val (title, heading, message) = when (status) {
            404 -> Triple(
                "Page Not Found",
                "404 - Page Not Found",
                "The page you're looking for doesn't exist."
            )
            403 -> Triple(
                "Access Denied",
                "403 - Access Denied",
                "You don't have permission to access this resource."
            )
            401 -> Triple(
                "Unauthorized",
                "401 - Unauthorized",
                "You need to be authenticated to access this resource."
            )
            500 -> Triple(
                "Internal Server Error",
                "500 - Internal Server Error",
                "Something went wrong on our end. Please try again later."
            )
            else -> Triple(
                "Error",
                "Error $status",
                errorMessage ?: "An unexpected error occurred."
            )
        }
        
        val errorHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title - Chicken Calculator</title>
                <style>
                    body {
                        font-family: system-ui, -apple-system, sans-serif;
                        margin: 0;
                        padding: 0;
                        background: #f5f5f5;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .error-container {
                        background: white;
                        padding: 40px;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        max-width: 500px;
                        width: 90%;
                        text-align: center;
                    }
                    h1 {
                        color: #d32f2f;
                        margin: 0 0 20px 0;
                        font-size: 24px;
                    }
                    p {
                        color: #666;
                        margin: 0 0 30px 0;
                        line-height: 1.6;
                    }
                    .error-code {
                        font-size: 72px;
                        font-weight: bold;
                        color: #e0e0e0;
                        margin: 0 0 20px 0;
                    }
                    .links {
                        display: flex;
                        gap: 15px;
                        justify-content: center;
                        flex-wrap: wrap;
                    }
                    a {
                        display: inline-block;
                        padding: 10px 20px;
                        background: #007bff;
                        color: white;
                        text-decoration: none;
                        border-radius: 4px;
                        transition: background 0.3s;
                    }
                    a:hover {
                        background: #0056b3;
                    }
                    .details {
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #e0e0e0;
                        font-size: 12px;
                        color: #999;
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-code">$status</div>
                    <h1>$heading</h1>
                    <p>$message</p>
                    <div class="links">
                        <a href="/">Go to Home</a>
                        <a href="/admin">Admin Portal</a>
                        <a href="/main-calculator">Main Calculator</a>
                    </div>
                    ${if (errorUri != null) {
                        """<div class="details">
                            Requested path: $errorUri
                        </div>"""
                    } else ""}
                </div>
            </body>
            </html>
        """.trimIndent()
        
        return ResponseEntity
            .status(HttpStatus.valueOf(status))
            .contentType(MediaType.TEXT_HTML)
            .body(errorHtml)
    }
}