package com.example.chickencalculator.error

import org.slf4j.LoggerFactory
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest

@Primary
@Component
class TappingErrorAttributes : DefaultErrorAttributes() {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun getError(webRequest: WebRequest): Throwable? {
    val ex = super.getError(webRequest)
    if (ex != null) {
      log.error("TappingErrorAttributes captured root exception", ex)
    } else {
      log.error("TappingErrorAttributes: no exception attached to error request")
    }
    return ex
  }

  override fun getErrorAttributes(webRequest: WebRequest, options: ErrorAttributeOptions): MutableMap<String, Any> {
    // Always include message + stacktrace for diagnostics
    val augmented = options.including(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.STACK_TRACE)
    return super.getErrorAttributes(webRequest, augmented)
  }
}