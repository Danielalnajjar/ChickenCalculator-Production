package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Debug controller with absolute minimal functionality
 */
@RestController
class DebugController {
    
    private val logger = LoggerFactory.getLogger(DebugController::class.java)
    
    @GetMapping("/debug")
    fun debug(): String {
        logger.info("🐛 Debug endpoint called")
        return "Debug working"
    }
    
    @GetMapping("/debug-json")
    fun debugJson(): Map<String, String> {
        logger.info("🐛 Debug JSON endpoint called")
        return mapOf("status" to "ok")
    }
    
    @GetMapping("/debug-response")
    fun debugResponse(): ResponseEntity<String> {
        logger.info("🐛 Debug ResponseEntity endpoint called")
        return ResponseEntity.ok("Debug ResponseEntity working")
    }
}