package com.kling.waic.component.helper

import com.kling.waic.component.config.AdminConfig
import com.kling.waic.component.entity.Token
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import redis.clients.jedis.commands.JedisCommands
import java.time.Instant
import java.util.*

@Component
class TokenHelper(
    private val jedis: JedisCommands,
) {
    @Volatile
    private var latestToken: Token? = null

    fun getLatest(adminConfig: AdminConfig): Token {
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
                    nowInLock.plusSeconds(5),
                    nowInLock.plusSeconds(adminConfig.tokenExpireInSeconds)
                )
                latestToken = newToken
                jedis.setex(newToken.value,
                    adminConfig.tokenExpireInSeconds,
                    ObjectMapperUtils.Companion.toJSON(newToken))
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
}