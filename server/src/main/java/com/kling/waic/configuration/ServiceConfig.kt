package com.kling.waic.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.Jedis

@Configuration
open class ServiceConfig(
    @param:Value("\${jedis.host}") private val host: String,
    @param:Value("\${jedis.port}") private val port: Int,
) {

    @Bean
    open fun jedis(): Jedis {
        return Jedis(host, port)
    }
}