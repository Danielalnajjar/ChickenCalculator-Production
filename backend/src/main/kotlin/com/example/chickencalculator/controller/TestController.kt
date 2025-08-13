package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Simple test controller to debug servlet exceptions
 */
// @RestController  // Temporarily disabled to debug servlet exceptions
class TestController {
    private val logger = LoggerFactory.getLogger(TestController::class.java)
    
    @GetMapping("/test")
    fun test(): ResponseEntity<String> {
        logger.info("🧪 Test endpoint called")
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body("Test successful")
    }
    
    @GetMapping("/test-html")
    fun testHtml(): ResponseEntity<String> {
        logger.info("🧪 Test HTML endpoint called")
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body><h1>Test Page</h1></body>
            </html>
        """.trimIndent()
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html)
    }
}