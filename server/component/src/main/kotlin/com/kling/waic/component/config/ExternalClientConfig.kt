package com.kling.waic.component.config

import com.kling.waic.component.utils.Slf4j.Companion.log
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

@Configuration
class ExternalClientConfig(
    @param:Value("\${KLING_PROXY_HOST:localhost}") private val proxyHost: String,
    @param:Value("\${KLING_PROXY_PORT:6379}") private val proxyPort: Int,
    @param:Value("\${WAIC_KLING_USE_PROXY:false}") private val useProxy: Boolean,
) {

    @Bean
    open fun okHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)

        if (useProxy) {
            builder.proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(proxyHost, proxyPort)
                )
            )
            log.info("Using proxy for okHttpClient: $proxyHost:$proxyPort")
        }

        log.info("OkHttpClient configured with timeouts - connect: 30s, write: 60s, read: 120s, call: 180s")
        return builder.build()
    }
}