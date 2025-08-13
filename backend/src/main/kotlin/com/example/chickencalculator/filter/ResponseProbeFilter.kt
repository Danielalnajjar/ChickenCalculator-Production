package com.example.chickencalculator.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // run early on REQUEST (after ErrorTapFilter on ERROR)
class ResponseProbeFilter : OncePerRequestFilter() {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun shouldNotFilterErrorDispatch() = false

  override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
    val wrapper = object : HttpServletResponseWrapper(res) {
      override fun setStatus(sc: Int) {
        log.debug("ResponseProbe: setStatus({}) for {} {}", sc, req.method, req.requestURI)
        super.setStatus(sc)
      }
      override fun sendError(sc: Int) {
        log.error("ResponseProbe: sendError({}) for {} {}", sc, req.method, req.requestURI)
        super.sendError(sc)
      }
      override fun sendError(sc: Int, msg: String?) {
        log.error("ResponseProbe: sendError({}, {}) for {} {}", sc, msg, req.method, req.requestURI)
        super.sendError(sc, msg)
      }
      override fun sendRedirect(location: String?) {
        log.debug("ResponseProbe: sendRedirect({}) for {} {}", location, req.method, req.requestURI)
        super.sendRedirect(location)
      }
      override fun flushBuffer() {
        log.debug("ResponseProbe: flushBuffer (committed={}) for {} {}", isCommitted, req.method, req.requestURI)
        super.flushBuffer()
      }
    }
    try {
      chain.doFilter(req, wrapper)
    } finally {
      log.debug("ResponseProbe: completed {} {} status={}", req.method, req.requestURI, wrapper.status)
    }
  }
}