package com.example.chickencalculator.controller

import com.example.chickencalculator.config.ApiVersionConfig
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("${ApiVersionConfig.API_VERSION}/admin")
@Deprecated("Use AdminAuthController and AdminLocationController instead")
class AdminController {
    
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "healthy",
            "message" to "Admin system operational"
        ))
    }
}