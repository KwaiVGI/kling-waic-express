package com.kling.waic.repository

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
class JWTRepository(
    @Value("\${waic.open-api.access-key}")
    private val waicOpenApiAccessKey: String,
    @Value("\${waic.open-api.secret-key}")
    private val waicOpenApiSecretKey: String,
    @Value("\${waic.jwt.expireAtInSeconds}")
    private val expireAtInSeconds: Int,
    @Value("\${waic.jwt.notBeforeInSeconds}")
    private val notBeforeInSeconds: Int
) {

    @Volatile
    private var latestJWT: String? = null

    @Volatile
    private var expireAtMillis: Long? = null

    fun getLatest(): String {
        val now = Instant.now().toEpochMilli()

        if (latestJWT != null
            && expireAtMillis != null
            && expireAtMillis!! > now + SAFETY_MARGIN_MS
        ) {
            return latestJWT!!
        }

        synchronized(this) {
            val refreshedNow = Instant.now().toEpochMilli()
            if (latestJWT == null
                || expireAtMillis == null
                || expireAtMillis!! <= refreshedNow + SAFETY_MARGIN_MS
            ) {
                val (newJwt, newExpireAt) = generateJWT()
                latestJWT = newJwt
                expireAtMillis = newExpireAt
            }
            return latestJWT!!
        }
    }

    private fun generateJWT(): Pair<String, Long> {
        val currentMillis = Instant.now().toEpochMilli()

        val expireAt = currentMillis + expireAtInSeconds * THOUSAND
        val notBeforeAt = currentMillis + notBeforeInSeconds * THOUSAND

        val algorithm = Algorithm.HMAC256(waicOpenApiSecretKey)
        val jwt = JWT.create()
            .withIssuer(waicOpenApiAccessKey)
            .withHeader(mapOf("alg" to "HS256"))
            .withExpiresAt(Date(expireAt))
            .withNotBefore(Date(notBeforeAt))
            .sign(algorithm)

        return jwt to expireAt
    }

    companion object {
        private const val THOUSAND = 1000L
        private const val SAFETY_MARGIN_MS = 10 * THOUSAND // Refresh 10 seconds early
    }
}
