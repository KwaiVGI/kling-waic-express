import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.JedisCluster
import kotlin.test.Test
import kotlin.test.assertEquals


class JedisClientTest {

    @Test
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

        assertEquals(jedisCluster.set("key", "value"), "OK")
        assertEquals(jedisCluster.get("key"), "value")
    }
}