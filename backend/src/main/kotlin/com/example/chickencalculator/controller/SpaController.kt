package com.example.chickencalculator.controller

import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SpaController(
    private val resourceLoader: ResourceLoader
) {

    @GetMapping("/admin", "/admin/{path:[^\\.]*}")
    fun serveAdminApp(@PathVariable(required = false) path: String?): ResponseEntity<Resource> {
        val adminIndexResource = resourceLoader.getResource("file:/app/static/admin/index.html")
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(adminIndexResource)
    }

    @GetMapping("/location/{slug}", "/location/{slug}/{path:[^\\.]*}")
    fun serveLocationApp(
        @PathVariable slug: String,
        @PathVariable(required = false) path: String?
    ): ResponseEntity<Resource> {
        val appIndexResource = resourceLoader.getResource("file:/app/static/app/index.html")
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(appIndexResource)
    }
}