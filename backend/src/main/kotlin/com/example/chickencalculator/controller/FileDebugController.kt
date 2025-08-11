package com.example.chickencalculator.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
@RequestMapping("/api/debug")
class FileDebugController {
    
    data class FileInfo(
        val path: String,
        val exists: Boolean,
        val isDirectory: Boolean,
        val size: Long,
        val files: List<String>? = null
    )
    
    @GetMapping("/files")
    fun listFiles(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // Check main app static files
        val appDir = File("/app/static/app")
        result["mainApp"] = FileInfo(
            path = appDir.absolutePath,
            exists = appDir.exists(),
            isDirectory = appDir.isDirectory,
            size = if (appDir.isFile) appDir.length() else 0,
            files = if (appDir.isDirectory) appDir.listFiles()?.map { it.name }?.take(10) else null
        )
        
        val appStaticDir = File("/app/static/app/static")
        result["mainAppStatic"] = FileInfo(
            path = appStaticDir.absolutePath,
            exists = appStaticDir.exists(),
            isDirectory = appStaticDir.isDirectory,
            size = if (appStaticDir.isFile) appStaticDir.length() else 0,
            files = if (appStaticDir.isDirectory) appStaticDir.listFiles()?.map { it.name }?.take(10) else null
        )
        
        // Check admin portal static files
        val adminDir = File("/app/static/admin")
        result["adminPortal"] = FileInfo(
            path = adminDir.absolutePath,
            exists = adminDir.exists(),
            isDirectory = adminDir.isDirectory,
            size = if (adminDir.isFile) adminDir.length() else 0,
            files = if (adminDir.isDirectory) adminDir.listFiles()?.map { it.name }?.take(10) else null
        )
        
        val adminStaticDir = File("/app/static/admin/static")
        result["adminPortalStatic"] = FileInfo(
            path = adminStaticDir.absolutePath,
            exists = adminStaticDir.exists(),
            isDirectory = adminStaticDir.isDirectory,
            size = if (adminStaticDir.isFile) adminStaticDir.length() else 0,
            files = if (adminStaticDir.isDirectory) adminStaticDir.listFiles()?.map { it.name }?.take(10) else null
        )
        
        // Check specific JS directories
        val appJsDir = File("/app/static/app/static/js")
        if (appJsDir.exists() && appJsDir.isDirectory) {
            result["mainAppJs"] = appJsDir.listFiles()?.take(5)?.map { 
                mapOf(
                    "name" to it.name,
                    "size" to it.length()
                )
            } ?: emptyList<Map<String, Any>>()
        }
        
        val adminJsDir = File("/app/static/admin/static/js")
        if (adminJsDir.exists() && adminJsDir.isDirectory) {
            result["adminPortalJs"] = adminJsDir.listFiles()?.take(5)?.map { 
                mapOf(
                    "name" to it.name,
                    "size" to it.length()
                )
            } ?: emptyList<Map<String, Any>>()
        }
        
        // Check if index.html files exist
        val appIndex = File("/app/static/app/index.html")
        result["mainAppIndex"] = mapOf(
            "path" to appIndex.absolutePath,
            "exists" to appIndex.exists(),
            "size" to if (appIndex.exists()) appIndex.length() else 0
        )
        
        val adminIndex = File("/app/static/admin/index.html")
        result["adminPortalIndex"] = mapOf(
            "path" to adminIndex.absolutePath,
            "exists" to adminIndex.exists(),
            "size" to if (adminIndex.exists()) adminIndex.length() else 0
        )
        
        // System info
        result["systemInfo"] = mapOf(
            "workingDirectory" to System.getProperty("user.dir"),
            "javaVersion" to System.getProperty("java.version"),
            "osName" to System.getProperty("os.name")
        )
        
        return result
    }
    
    @GetMapping("/paths")
    fun checkPaths(): Map<String, List<String>> {
        val paths = listOf(
            "/app/static",
            "/app/static/app",
            "/app/static/app/static",
            "/app/static/app/static/js",
            "/app/static/app/static/css",
            "/app/static/admin",
            "/app/static/admin/static",
            "/app/static/admin/static/js",
            "/app/static/admin/static/css"
        )
        
        val result = mutableMapOf<String, List<String>>()
        
        result["existing"] = paths.filter { File(it).exists() }
        result["missing"] = paths.filter { !File(it).exists() }
        
        return result
    }
}