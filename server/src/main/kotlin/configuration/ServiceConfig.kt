package com.klingai.express.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.Jedis

@Configuration
open class ServiceConfig {

    @Bean
    open fun jedis(): Jedis {
        return Jedis("host.docker.internal", 6379)
    }
}