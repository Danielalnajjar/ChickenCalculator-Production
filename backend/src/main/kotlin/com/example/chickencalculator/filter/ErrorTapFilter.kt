package com.example.chickencalculator.filter

import jakarta.servlet.DispatcherType
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// @Component  // TEMPORARILY DISABLED FOR DEBUGGING
@Order(Ordered.HIGHEST_PRECEDENCE) // first on ERROR dispatch
class ErrorTapFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Only run for ERROR dispatches
        return request.dispatcherType != DispatcherType.ERROR
    }

    override fun shouldNotFilterErrorDispatch(): Boolean = false

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val status = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        val uri    = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)
        val ex     = req.getAttribute(RequestDispatcher.ERROR_EXCEPTION) as? Throwable
        val msg    = req.getAttribute(RequestDispatcher.ERROR_MESSAGE) as? String
        log.error("ERROR DISPATCH captured: status={}, uri={}, message={}", status, uri, msg, ex)
        chain.doFilter(req, res)
    }
}