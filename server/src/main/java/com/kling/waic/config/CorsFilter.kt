package com.kling.waic.config

import com.kling.waic.utils.Slf4j.Companion.log
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorsFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val origin = httpRequest.getHeader("Origin")
        
        // Setting CORS Header
        if (origin != null) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin)
        } else {
            httpResponse.setHeader("Access-Control-Allow-Origin", "*")
        }

        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH")
        httpResponse.setHeader("Access-Control-Allow-Headers", "*")
//        httpResponse.setHeader("Access-Control-Expose-Headers", "Authorization")
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true")
        httpResponse.setHeader("Access-Control-Max-Age", "3600")

        // Processing Precheck
        if ("OPTIONS".equals(httpRequest.method, ignoreCase = true)) {
            log.info("CORS preflight request: {} {}", httpRequest.method, httpRequest.requestURI)
            httpResponse.status = HttpServletResponse.SC_OK
            return
        }

        chain.doFilter(request, response)
    }
}
