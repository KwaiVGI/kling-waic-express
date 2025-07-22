package com.kling.waic.helper

import com.kling.waic.entity.Token
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import redis.clients.jedis.commands.JedisCommands
import java.time.Instant
import java.util.*

@Component
class TokenHelper(
    private val jedis: JedisCommands
) {
    @Volatile
    private var latestToken: Token? = null

    fun getLatest(): Token {
        val now = Instant.now()
        val current = latestToken
        if (current != null && current.refreshTime > now) {
            return current
        }

        synchronized(this) {
            val previous = latestToken
            val nowInLock = Instant.now()

            if (previous == null || previous.refreshTime <= nowInLock) {
                val newToken = Token(
                    (previous?.id ?: 0) + 1,
                    UUID.randomUUID().toString(),
                    nowInLock,
                    nowInLock.plusSeconds(EXPIRE_IN_SECONDS - 5),
                    nowInLock.plusSeconds(EXPIRE_IN_SECONDS)
                )
                latestToken = newToken
                jedis.setex(newToken.value, EXPIRE_IN_SECONDS, ObjectMapperUtils.Companion.toJSON(newToken))
                log.info("Generated and saved new token: id={}, name={}", newToken.id, newToken.value)
            }
            return latestToken!!
        }
    }

    fun getByName(name: String): Token? {
        val valueStr = jedis.get(name)
        return ObjectMapperUtils.Companion.fromJSON(valueStr, Token::class.java)
    }

    fun validate(token: String): Boolean {
        val storedToken = getByName(token)
        return storedToken?.let { Instant.now().isBefore(it.expireTime) } ?: false
    }

    companion object {
        // todo: change to 10min before WAIC
        const val EXPIRE_IN_SECONDS: Long = 60 * 60 * 5
    }
}