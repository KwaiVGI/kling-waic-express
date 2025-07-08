package com.kling.waic.repositories

import com.kling.waic.entities.Token
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Repository
import redis.clients.jedis.Jedis
import java.time.Instant
import java.util.*

@Repository
class TokenRepository(
    private val jedis: Jedis
) {
    var latestToken: Token? = null

    fun getLatest(): Token {
        val current = latestToken
        if (current != null && current.refreshTime > Instant.now()) {
            return current
        }

        synchronized(this) {
            val recheck = latestToken
            if (recheck == null || recheck.refreshTime <= Instant.now()) {
                log.info("Generate new token, recheck: {}", recheck)
                val newToken = Token(
                    (recheck?.id ?: 0) + 1,
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    Instant.now().plusSeconds(5),
                    Instant.now().plusSeconds(EXPIRE_IN_SECONDS)
                )
                latestToken = newToken
                jedis.setex(newToken.name, EXPIRE_IN_SECONDS, ObjectMapperUtils.toJSON(newToken))
            }
            return latestToken!!
        }
    }

    fun getByName(name: String): Token? {
        val valueStr = jedis.get(name)
        return ObjectMapperUtils.fromJSON(valueStr, Token::class.java)
    }

    fun validate(token: String): Boolean {
        val t = getByName(token) ?: return false
        return Instant.now() < t.expireTime
    }

    companion object {
        const val EXPIRE_IN_SECONDS: Long = 60 * 10
    }
}
