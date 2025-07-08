package com.kling.waic.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.Jedis

@Configuration
open class ServiceConfig {

    @Bean
    open fun jedis(): Jedis {
        return Jedis("172.17.0.1", 6379)
    }
}