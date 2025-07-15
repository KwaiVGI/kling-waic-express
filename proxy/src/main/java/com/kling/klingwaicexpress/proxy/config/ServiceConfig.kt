package com.kling.klingwaicexpress.proxy.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.net.InetSocketAddress

@Configuration
open class ServiceConfig(
    @Value("\${kling-waic.proxy.host}")
    private val proxyHost: String,
    @Value("\${kling-waic.proxy.port}")
    private val proxyPort: Int
) {

    @Bean
    open fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().proxy { spec ->
                        spec.type(ProxyProvider.Proxy.HTTP)
                            .address(InetSocketAddress(proxyHost, proxyPort))
                    }
                )
            )
    }
}