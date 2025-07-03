package com.klingai.express.repositories

import com.klingai.express.entities.Token
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
class TokenRepository (
    val tokens: MutableMap<String, Token> = mutableMapOf(),
    var latestToken: Token? = null
) {
    fun getLatest(): Token {
        synchronized(this) {
            if (this.latestToken == null ||
                this.latestToken!!.refreshTime <= Instant.now()) {
                this.latestToken = Token((this.latestToken?.id ?: 0) + 1,
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    Instant.now().plusSeconds(5),
                    Instant.now().plusSeconds(60 * 10))
                this.tokens[this.latestToken!!.name] = this.latestToken!!
            }
            return this.latestToken!!
        }
    }

    fun getByName(name: String): Token? {
        synchronized(this) {
            return this.tokens[name]
        }
    }

    fun validate(token: String): Boolean {
        val t = this.getByName(token)
        if (t == null) {
            return false
        }
        return Instant.now() < t.expireTime
    }
}
