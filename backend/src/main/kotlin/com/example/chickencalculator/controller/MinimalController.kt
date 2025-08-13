package com.example.chickencalculator.controller

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Minimal controller to test basic Spring Boot functionality
 * Only available in development profile for security
 */
@RestController
@Profile("dev")
class MinimalController {
    
    @GetMapping("/minimal")
    fun minimal(): Map<String, String> {
        return mapOf("status" to "ok", "message" to "Minimal controller working")
    }
    
    @GetMapping("/minimal-string")
    fun minimalString(): String {
        return "OK"
    }
}