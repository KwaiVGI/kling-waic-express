package com.kling.waic.component.helper

import com.kling.waic.component.entity.AdminConfig
import com.kling.waic.component.entity.TaskType
import com.kling.waic.component.entity.Token
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.component.utils.ThreadContextUtils
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

    fun getLatest(type : TaskType, adminConfig: AdminConfig): Token {
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
                    nowInLock.plusSeconds(adminConfig.imageTokenExpireInSeconds),
                    activity = ThreadContextUtils.getActivity()
                )
                latestToken = newToken
                val tokenExpireInSeconds = when (type) {
                    TaskType.STYLED_IMAGE -> adminConfig.imageTokenExpireInSeconds
                    TaskType.VIDEO_EFFECT -> adminConfig.videoTokenExpireInSeconds
                }
                jedis.setex(newToken.value,
                    tokenExpireInSeconds,
                    ObjectMapperUtils.Companion.toJSON(newToken))
                log.debug("Generated and saved new token: id={}, type={}, name={}",
                    newToken.id, type, newToken.value)
            }
            return latestToken!!
        }
    }

    fun getByName(name: String): Token? {
        val valueStr = jedis.get(name)
        return ObjectMapperUtils.fromJSON(valueStr, Token::class.java)
    }

    fun validate(activity: String, token: String): Boolean {
        val storedToken = getByName(token)
        return storedToken?.let {
            (activity.isEmpty() || activity == storedToken.activity)
                    && Instant.now().isBefore(it.expireTime)
        } ?: false
    }
}