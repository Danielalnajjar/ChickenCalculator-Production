package com.example.chickencalculator.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@Profile("dev")
@RestController
class DebugMvcController(
    private val ctx: ApplicationContext
) {
    @Autowired(required = false)
    private val objectMapper: ObjectMapper? = null
    
    @GetMapping("/debug/converters")
    fun converters(): ResponseEntity<Map<String, Any>> {
        return try {
            val adapter = ctx.getBean(RequestMappingHandlerAdapter::class.java)
            val list = adapter.messageConverters.map { 
                mapOf(
                    "class" to it::class.java.name,
                    "simpleName" to it::class.java.simpleName,
                    "supportedMediaTypes" to it.supportedMediaTypes.map { mt -> mt.toString() }
                )
            }
            ResponseEntity.ok(
                mapOf(
                    "converters" to list,
                    "converterCount" to list.size,
                    "hasObjectMapper" to (objectMapper != null),
                    "objectMapperModules" to (objectMapper?.registeredModuleIds ?: emptySet())
                )
            )
        } catch (e: Exception) {
            ResponseEntity.ok(
                mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "hasObjectMapper" to (objectMapper != null)
                )
            )
        }
    }
}