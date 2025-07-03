package com.klingai.express.repositories

import com.klingai.express.entities.Token
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Repository
class TokenRepository (
    val tokens: MutableMap<String, Token> = ConcurrentHashMap(),
    var latestToken: Token? = null
) {
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
                this.tokens[newToken.name] = newToken
            }
            return this.latestToken!!
        }
    }

    fun getByName(name: String): Token? {
        return this.tokens[name]
    }

    fun validate(token: String): Boolean {
        val t = this.getByName(token)
        if (t == null) {
            return false
        }
        return Instant.now() < t.expireTime
    }
}
