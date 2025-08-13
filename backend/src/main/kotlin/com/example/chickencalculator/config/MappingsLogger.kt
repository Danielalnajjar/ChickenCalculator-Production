package com.example.chickencalculator.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * MappingsLogger - Diagnostic configuration to log all registered request mappings.
 * 
 * This configuration helps diagnose servlet 500 errors by:
 * 1. Confirming which controllers and mappings are actually registered
 * 2. Identifying any mapping conflicts or registration issues
 * 3. Providing visibility into Spring MVC's routing configuration
 * 
 * Logs all registered request mappings at application startup.
 */
@Configuration
class MappingsLogger {

    private val logger = LoggerFactory.getLogger(MappingsLogger::class.java)

    @Bean
    fun logRequestMappings(requestMappingHandlerMapping: RequestMappingHandlerMapping): ApplicationRunner {
        return ApplicationRunner {
            logger.info("=== REQUEST MAPPING REGISTRATION ANALYSIS ===")
            
            val handlerMethods = requestMappingHandlerMapping.handlerMethods
            
            logger.info("Total registered request mappings: ${handlerMethods.size}")
            
            handlerMethods.forEach { (requestMappingInfo, handlerMethod) ->
                val patterns = requestMappingInfo.patternsCondition?.patterns ?: emptySet()
                val methods = requestMappingInfo.methodsCondition?.methods ?: emptySet()
                val consumes = requestMappingInfo.consumesCondition?.expressions ?: emptySet()
                val produces = requestMappingInfo.producesCondition?.expressions ?: emptySet()
                
                val controllerClass = handlerMethod.beanType.simpleName
                val methodName = handlerMethod.method.name
                val returnType = handlerMethod.method.returnType.simpleName
                
                logger.info("MAPPING: {} {} -> {}.{}() returns {}", 
                    methods.joinToString(",") { it.name },
                    patterns.joinToString(","),
                    controllerClass,
                    methodName,
                    returnType
                )
                
                if (consumes.isNotEmpty()) {
                    logger.info("  Consumes: {}", consumes.joinToString(","))
                }
                if (produces.isNotEmpty()) {
                    logger.info("  Produces: {}", produces.joinToString(","))
                }
            }
            
            // Log our specific controllers of interest
            val controllersOfInterest = listOf(
                "ProbeController",
                "TestController", 
                "HealthController",
                "AdminAuthController",
                "ChickenCalculatorController"
            )
            
            logger.info("=== CONTROLLERS OF INTEREST ===")
            controllersOfInterest.forEach { controllerName ->
                val mappingsForController = handlerMethods.filter { 
                    it.value.beanType.simpleName == controllerName 
                }
                
                if (mappingsForController.isNotEmpty()) {
                    logger.info("✓ {} registered with {} mappings", controllerName, mappingsForController.size)
                    mappingsForController.forEach { (mapping, method) ->
                        val patterns = mapping.patternsCondition?.patterns ?: emptySet()
                        val httpMethods = mapping.methodsCondition?.methods ?: emptySet()
                        logger.info("  - {} {} -> {}()", 
                            httpMethods.joinToString(",") { it.name },
                            patterns.joinToString(","),
                            method.method.name
                        )
                    }
                } else {
                    logger.warn("✗ {} NOT REGISTERED - check component scan or annotations", controllerName)
                }
            }
            
            logger.info("=== END REQUEST MAPPING ANALYSIS ===")
        }
    }
}