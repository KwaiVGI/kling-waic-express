package com.kling.waic.component.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.*

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
open class RequestIdFilter : Filter {
    companion object {
        const val REQUEST_ID = "requestId"
    }

    override fun doFilter(request: ServletRequest,
                          response: ServletResponse,
                          chain: FilterChain) {
        try {
            val requestId: String = UUID.randomUUID().toString()
            MDC.put(REQUEST_ID, requestId)
            chain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID)
        }
    }
}