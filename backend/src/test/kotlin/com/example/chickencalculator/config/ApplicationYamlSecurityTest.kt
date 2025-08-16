package com.example.chickencalculator.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.InputStream

class ApplicationYamlSecurityTest {

    @Test
    fun `management exposure includes prometheus and excludes loggers`() {
        val isr: InputStream = this::class.java.classLoader
            .getResourceAsStream("application.yml")
            ?: throw IllegalStateException("application.yml not found on classpath")

        @Suppress("UNCHECKED_CAST")
        val root = Yaml().loadAll(isr).first() as Map<String, Any?>

        fun m(key: String, map: Map<String, Any?>?): Map<String, Any?> =
            (map?.get(key) as? Map<String, Any?>) ?: emptyMap()

        val includeNode = m("exposure", m("web", m("endpoints", m("management", root))))["include"]

        val includes: List<String> = when (includeNode) {
            is String -> includeNode.split(',').map { it.trim() }
            is Iterable<*> -> includeNode.filterIsInstance<String>()
            else -> emptyList()
        }

        assertTrue("prometheus" in includes, "Expected 'prometheus' in actuator exposure include list")
        assertTrue("loggers" !in includes, "Must NOT expose 'loggers' actuator endpoint")
    }
}