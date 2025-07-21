package com.kling.waic.test.redis

import org.junit.jupiter.api.Test
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.JedisCluster
import kotlin.test.assertEquals


class JedisTest {

    @Test
    fun testJedis() {
        val maxAttempts = 5

        val clientConfig: JedisClientConfig? = DefaultJedisClientConfig.builder()
            .ssl(true)
            .build()

        val jedisCluster = JedisCluster(
            HostAndPort("kling-waic-fblxb2.serverless.cnn1.cache.amazonaws.com.cn", 6379),
            clientConfig,
            maxAttempts,
            ConnectionPoolConfig()
        )

        // Set and assert key-value
        assertEquals(jedisCluster.set("key", "value"), "OK")
        // Get and assert the value
        assertEquals(jedisCluster.get("key"), "value")
    }

    @Test
    fun testHaha() {
        val haha = "haha"
        println(haha)
        assertEquals(haha, "haha")
    }
}