package com.example.chickencalculator.util

import jakarta.servlet.http.HttpServletRequest

object PathUtil {
    fun normalizedPath(request: HttpServletRequest): String {
        val ctx = request.contextPath ?: ""
        val uri = request.requestURI ?: ""
        return if (ctx.isNotEmpty() && uri.startsWith(ctx)) uri.removePrefix(ctx) else uri
    }
}