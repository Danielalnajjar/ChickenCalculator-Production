package com.example.chickencalculator.error

import org.slf4j.LoggerFactory
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.context.request.ServletWebRequest
import jakarta.servlet.http.HttpServletRequest

@Controller
class PlainErrorController(
  private val errorAttributes: ErrorAttributes
) : ErrorController {
  private val log = LoggerFactory.getLogger(javaClass)

  // Force text/plain to bypass JSON converters entirely
  @RequestMapping(value = ["/error"], produces = [MediaType.TEXT_PLAIN_VALUE])
  fun error(req: HttpServletRequest): ResponseEntity<String> {
    val web = ServletWebRequest(req)
    val opts = ErrorAttributeOptions.defaults()
      .including(ErrorAttributeOptions.Include.MESSAGE)
      .including(ErrorAttributeOptions.Include.STACK_TRACE)
      .including(ErrorAttributeOptions.Include.EXCEPTION)

    val attrs = errorAttributes.getErrorAttributes(web, opts)
    val status = (attrs["status"] as? Int) ?: 500
    val path = attrs["path"] ?: req.requestURI
    val error = attrs["error"] ?: ""
    val message = attrs["message"] ?: ""
    val trace = (attrs["trace"] as? String) ?: ""

    val body = buildString {
      appendLine("status=$status path=$path error=$error")
      if (message.toString().isNotBlank()) appendLine("message=$message")
      if (trace.isNotBlank()) {
        appendLine("trace:")
        appendLine(trace)
      }
    }
    log.error("PlainErrorController responding (status={}) for {}", status, path)
    return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(body.ifBlank { "error $status at $path" })
  }
}