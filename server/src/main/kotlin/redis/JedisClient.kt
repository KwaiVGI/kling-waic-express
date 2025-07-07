package com.klingai.express.redis

import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.JedisCluster


class JedisClient {

    fun test() {

        val maxAttempts = 5

        val clientConfig: JedisClientConfig = DefaultJedisClientConfig.builder()
            .ssl(true)
            .build()

        val jedisCluster = JedisCluster(
            HostAndPort("kling-waic-fblxb2.serverless.cnn1.cache.amazonaws.com.cn", 6379),
            clientConfig,
            maxAttempts,
            ConnectionPoolConfig()
        )

        jedisCluster.set("key", "value")

    }
}