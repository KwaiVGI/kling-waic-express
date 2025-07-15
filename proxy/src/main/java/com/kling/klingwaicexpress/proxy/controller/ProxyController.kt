package com.kling.klingwaicexpress.proxy.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@RestController
class ProxyController(
    @Value("\${kling-waic.server.host}")
    private val host: String,
    @Value("\${kling-waic.server.port}")
    private val port: Int,
    private val webClientBuilder: WebClient.Builder
) {

    @RequestMapping("/**",
        method = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH])
    fun proxy(request: HttpServletRequest,
              entity: HttpEntity<String>): ResponseEntity<Mono<String>> {
        val method = HttpMethod.valueOf(request.method)
        val path = request.requestURI.replace("/proxy", "/api")
        val query = request.queryString?.let { "?$it" } ?: ""
        val url = "http://$host:$port$path$query"

        val headers = HttpHeaders()
        request.headerNames.toList().forEach {
            headers[it] = request.getHeaders(it).toList()
        }

        return ResponseEntity.status(HttpStatus.OK).body(
            webClientBuilder.build()
                .method(method)
                .uri(url)
                .headers { it.putAll(headers) }
                .bodyValue(entity.body ?: "")
                .retrieve()
                .bodyToMono(String::class.java)
        )
    }
}