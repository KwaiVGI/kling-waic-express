package com.kling.klingwaicexpress.proxy.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.Part
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
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
    fun proxy(
        request: HttpServletRequest,
        entity: HttpEntity<String>
    ): ResponseEntity<Mono<String>> {
        val method = HttpMethod.valueOf(request.method)
        val path = request.requestURI.replace("/proxy", "/api")
        val query = request.queryString?.let { "?$it" } ?: ""
        val url = "http://$host:$port$path$query"

        val headers = HttpHeaders()
        request.headerNames.toList().forEach {
            headers[it] = request.getHeaders(it).toList()
        }

        val contentType = request.contentType ?: ""

        val bodyInserter: BodyInserter<*, in ClientHttpRequest> =
            if (contentType.startsWith("multipart/form-data")) {
            val builder = MultipartBodyBuilder()
            request.parts.forEach {
                val part = it as Part
                builder.part(part.name, part.inputStream)
                    .filename(part.submittedFileName)
                    .contentType(MediaType.parseMediaType(part.contentType))
            }
            BodyInserters.fromMultipartData(builder.build())
        } else {
            BodyInserters.fromValue(entity.body ?: "")
        }

        val responseMono = webClientBuilder.build()
            .method(method)
            .uri(url)
            .headers { it.putAll(headers) }
            .body(bodyInserter)
            .retrieve()
            .bodyToMono(String::class.java)

        return ResponseEntity.status(HttpStatus.OK).body(responseMono)
    }
}