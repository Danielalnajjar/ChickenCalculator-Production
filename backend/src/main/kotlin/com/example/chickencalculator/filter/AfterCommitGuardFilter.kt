package com.example.chickencalculator.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Profile("dev")  // Only active in development for debugging
@Order(Ordered.LOWEST_PRECEDENCE - 1) // run just before TailLogFilter
class AfterCommitGuardFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val guard = object : HttpServletResponseWrapper(res) {
            private fun trap(op: String, detail: String? = null) {
                if (isCommitted) {
                    val where = RuntimeException("after-commit write: $op ${detail ?: ""}")
                    log.error("AfterCommitGuard: {} {} {} {}", req.method, req.requestURI, op, detail ?: "", where)
                }
            }
            override fun addHeader(name: String, value: String) { trap("addHeader","$name=$value"); super.addHeader(name,value) }
            override fun setHeader(name: String, value: String) { trap("setHeader","$name=$value"); super.setHeader(name,value) }
            override fun addCookie(cookie: Cookie)             { trap("addCookie", cookie.name); super.addCookie(cookie) }
            override fun setStatus(sc: Int)                    { trap("setStatus","$sc"); super.setStatus(sc) }
            override fun sendError(sc: Int)                    { trap("sendError","$sc"); super.sendError(sc) }
            override fun sendError(sc: Int, msg: String?)      { trap("sendError","$sc,$msg"); super.sendError(sc, msg) }
            override fun sendRedirect(location: String?)       { trap("sendRedirect", location ?: ""); super.sendRedirect(location) }
            override fun setContentType(type: String?)         { trap("setContentType", type ?: ""); super.setContentType(type) }
            override fun setContentLength(len: Int)            { trap("setContentLength","$len"); super.setContentLength(len) }
            override fun setContentLengthLong(len: Long)       { trap("setContentLengthLong","$len"); super.setContentLengthLong(len) }
        }
        chain.doFilter(req, guard)
    }
}