package com.example.chickencalculator.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.LOWEST_PRECEDENCE) // run last
class TailLogFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(TailLogFilter::class.java)

    override fun shouldNotFilterErrorDispatch(): Boolean = true

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            chain.doFilter(request, response)
        } catch (t: Throwable) {
            log.error("TailLogFilter caught exception for ${request.method} ${request.requestURI}", t)
            throw t
        }
    }
}