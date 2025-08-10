package com.kling.waic.test.redis

import SpringBaseTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.clients.jedis.commands.JedisCommands
import kotlin.test.assertEquals


class JedisTest : SpringBaseTest() {

    @Autowired
    private lateinit var jedis: JedisCommands

    @Test
    fun testJedis() {
        // Set and assert key-value
        assertEquals(jedis.set("key", "value"), "OK")
        // Get and assert the value
        assertEquals(jedis.get("key"), "value")
    }

    @Test
    fun testHaha() {
        val haha = "haha"
        println(haha)
        assertEquals(haha, "haha")
    }
}