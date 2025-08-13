package com.example.chickencalculator.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * Debug controller to help diagnose servlet exceptions
 */
@RestController
class DebugController @Autowired constructor(
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping
) {
    
    @GetMapping("/debug/mappings")
    fun getMappings(): Map<String, Any> {
        val mappings = mutableListOf<String>()
        
        requestMappingHandlerMapping.handlerMethods.forEach { (mapping, method) ->
            val patterns = mapping.patternsCondition?.patterns ?: emptySet()
            val methods = mapping.methodsCondition?.methods ?: emptySet()
            patterns.forEach { pattern ->
                methods.forEach { httpMethod ->
                    mappings.add("$httpMethod $pattern -> ${method.beanType.simpleName}.${method.method.name}")
                }
            }
        }
        
        return mapOf(
            "totalMappings" to mappings.size,
            "mappings" to mappings.sorted()
        )
    }
    
    @GetMapping("/debug/simple")
    fun simple(): String {
        return "OK"
    }
}