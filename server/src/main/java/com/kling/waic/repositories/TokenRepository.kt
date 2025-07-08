package com.kling.waic.repositories

import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.entities.Token
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
        val current = this.latestToken
        if (current != null && current.refreshTime > Instant.now()) {
            return current
        }

        synchronized(this) {
            val recheck = this.latestToken
            if (recheck == null || recheck.refreshTime <= Instant.now()) {
                val newToken = Token(
                    (recheck?.id ?: 0) + 1,
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    Instant.now().plusSeconds(5),
                    Instant.now().plusSeconds(60 * 10)
                )
                this.latestToken = newToken
                jedis.set(newToken.name, ObjectMapperUtils.toJSON(newToken))
            }
            return this.latestToken!!
        }
    }

    fun getByName(name: String): Token? {
        val valueStr = jedis.get(name)
        return ObjectMapperUtils.fromJSON(valueStr, Token::class.java)
    }

    fun validate(token: String): Boolean {
        val t = this.getByName(token) ?: return false
        return Instant.now() < t.expireTime
    }
}
