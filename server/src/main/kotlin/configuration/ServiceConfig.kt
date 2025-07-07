package com.klingai.express.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.JedisCluster

@Configuration
open class ServiceConfig {

    @Bean
    open fun getJedisCluster(): JedisCluster {
        val maxAttempts = 5

        val clientConfig: JedisClientConfig = DefaultJedisClientConfig.builder()
            .ssl(true)
            .build()

        val jedisCluster = JedisCluster(
            HostAndPort("localhost", 6379),
            clientConfig,
            maxAttempts,
            ConnectionPoolConfig()
        )

        return jedisCluster
    }
}