package com.example.chickencalculator.controller

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ProbeController - Minimal debugging controller to establish baseline functionality.
 * 
 * This controller provides simple endpoints to test:
 * 1. Whether ANY controller can return responses successfully
 * 2. Exception handling behavior in the servlet container
 * 
 * Created for debugging servlet 500 errors where controllers execute successfully
 * but Spring MVC post-processing fails.
 */
@RestController
@RequestMapping("/probe")
@Profile("dev")
class ProbeController {

    private val logger = LoggerFactory.getLogger(ProbeController::class.java)

    /**
     * Simple successful response to test basic controller functionality.
     * Returns a minimal Map to avoid complex serialization issues.
     */
    @GetMapping("/ok")
    fun ok(): Map<String, Any> {
        logger.info("ProbeController.ok() called - testing basic response")
        return mapOf(
            "status" to "ok",
            "success" to true,
            "timestamp" to System.currentTimeMillis(),
            "message" to "ProbeController working correctly"
        )
    }

    /**
     * Deliberately throws an exception to test error handling.
     * This helps us understand if the issue is in successful responses
     * or affects all controller responses equally.
     */
    @GetMapping("/boom")
    fun boom(): Map<String, Any> {
        logger.info("ProbeController.boom() called - testing exception handling")
        throw RuntimeException("boom - deliberate test exception from ProbeController")
    }
}