package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

/**
 * Controller for handling the root path and serving the main application landing page.
 * Follows Spring Boot best practices for serving static content programmatically.
 */
@RestController
class RootController {
    private val logger = LoggerFactory.getLogger(RootController::class.java)
    
    /**
     * Handle the root path "/" and serve the main application.
     * First attempts to serve the actual index.html file if it exists.
     * Falls back to a simple redirect if the file is not found.
     */
    @GetMapping("/")
    fun handleRoot(): ResponseEntity<String> {
        logger.info("🏠 Handling root path request")
        
        // Temporarily just serve the fallback page to avoid file reading issues
        logger.info("📄 Serving fallback landing page (temporarily bypassing file reading)")
        return serveLandingPage()
    }
    
    /**
     * Alternative endpoint that returns a simple HTML landing page directly.
     * This demonstrates returning HTML as a string with @ResponseBody.
     */
    @GetMapping("/landing")
    fun serveLandingPage(): ResponseEntity<String> {
        logger.info("📄 Serving landing page")
        
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Chicken Calculator</title>
                <style>
                    body {
                        font-family: system-ui, -apple-system, sans-serif;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 40px 20px;
                        background: #f5f5f5;
                    }
                    .container {
                        background: white;
                        padding: 30px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    h1 { color: #333; }
                    .links {
                        margin-top: 30px;
                        display: grid;
                        gap: 15px;
                    }
                    a {
                        display: block;
                        padding: 15px;
                        background: #007bff;
                        color: white;
                        text-decoration: none;
                        border-radius: 4px;
                        text-align: center;
                        transition: background 0.3s;
                    }
                    a:hover {
                        background: #0056b3;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Welcome to Chicken Calculator</h1>
                    <p>Select a location to access the calculator:</p>
                    <div class="links">
                        <a href="/main-calculator">Main Calculator</a>
                        <a href="/admin">Admin Portal</a>
                        <a href="/api/health">API Health Check</a>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html)
    }
    
    /**
     * Simple redirect approach - redirects root to main calculator.
     * Uncomment and use this instead of handleRoot() if you prefer redirects.
     */
    // @GetMapping("/")
    // fun redirectToMainCalculator(): String {
    //     logger.info("↪️ Redirecting root to main calculator")
    //     return "redirect:/main-calculator"
    // }
}