package com.example.chickencalculator.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@Component
class MvcDiagnostics(
    private val ctx: ApplicationContext
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        try {
            val adapter = ctx.getBean(RequestMappingHandlerAdapter::class.java)
            val converters: List<HttpMessageConverter<*>> = adapter.messageConverters
            log.info("=== MVC MESSAGE CONVERTERS ({} total) ===", converters.size)
            converters.forEachIndexed { i, c -> 
                log.info("  #{} -> {} ({})", i, c::class.java.simpleName, c::class.java.name)
            }
            
            val hasObjectMapper = runCatching { 
                ctx.getBean(ObjectMapper::class.java) 
            }.isSuccess
            log.info("Jackson ObjectMapper bean present: {}", hasObjectMapper)
            
            if (hasObjectMapper) {
                val mapper = ctx.getBean(ObjectMapper::class.java)
                log.info("ObjectMapper modules: {}", mapper.registeredModuleIds)
            }
        } catch (e: Exception) {
            log.error("Failed to log MVC diagnostics", e)
        }
    }
}