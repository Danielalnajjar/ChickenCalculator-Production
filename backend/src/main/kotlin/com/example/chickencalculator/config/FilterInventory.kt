package com.example.chickencalculator.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class FilterInventory(private val ctx: ApplicationContext) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun logFilters() {
        val filters = ctx.getBeansOfType(OncePerRequestFilter::class.java)
            .values.sortedWith(AnnotationAwareOrderComparator.INSTANCE)
        log.info("=== Registered OncePerRequestFilter beans (ordered) ===")
        var i = 0
        for (f in filters) {
            val name = ctx.getBeanNamesForType(f::class.java).firstOrNull() ?: f::class.java.simpleName
            log.info("#{} -> {} ({})", i++, name, f::class.java.name)
        }
    }
}