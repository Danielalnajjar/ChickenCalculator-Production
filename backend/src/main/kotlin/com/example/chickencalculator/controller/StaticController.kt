package com.example.chickencalculator.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File

@Controller
class StaticController {

    @GetMapping("/admin", "/admin/")
    fun adminRoot(): ResponseEntity<Resource> {
        val resource = ClassPathResource("/static/admin/index.html")
        if (!resource.exists()) {
            // Try file system location
            val fileResource = File("/app/static/admin/index.html")
            if (fileResource.exists()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(org.springframework.core.io.FileSystemResource(fileResource))
            }
        }
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(resource)
    }

    @GetMapping("/admin/{path:[^.]*}")
    fun adminRoute(@PathVariable path: String): ResponseEntity<Resource> {
        // For all non-file routes, return index.html for React Router
        return adminRoot()
    }

    @GetMapping("/test.html")
    @ResponseBody
    fun testPage(): String {
        val resource = ClassPathResource("/static/test.html")
        return if (resource.exists()) {
            resource.inputStream.bufferedReader().use { it.readText() }
        } else {
            // Return inline test page if file doesn't exist
            """
            <!DOCTYPE html>
            <html>
            <head>
                <title>API Test</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    button { margin: 10px; padding: 10px; }
                    #result { margin-top: 20px; padding: 10px; border: 1px solid #ccc; }
                </style>
            </head>
            <body>
                <h1>Chicken Calculator API Test</h1>
                <button onclick="testAuth()">Test Authentication</button>
                <button onclick="testHealth()">Test Health</button>
                <div id="result"></div>
                <script>
                    function showResult(text) {
                        document.getElementById('result').innerText = text;
                    }
                    
                    async function testAuth() {
                        try {
                            const response = await fetch('/api/admin/auth/login', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({
                                    email: 'admin@yourcompany.com',
                                    password: 'Admin123!'
                                })
                            });
                            const data = await response.json();
                            showResult('Auth Response: ' + JSON.stringify(data, null, 2));
                        } catch(e) {
                            showResult('Error: ' + e.message);
                        }
                    }
                    
                    async function testHealth() {
                        try {
                            const response = await fetch('/api/health');
                            const data = await response.json();
                            showResult('Health Response: ' + JSON.stringify(data, null, 2));
                        } catch(e) {
                            showResult('Error: ' + e.message);
                        }
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        }
    }
}