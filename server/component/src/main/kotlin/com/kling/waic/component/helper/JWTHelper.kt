package com.kling.waic.component.helper

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kling.waic.component.selector.ActivityHandlerSelector
import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.component.utils.ThreadContextUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class JWTWrapper (
    val jwt: String,
    val expireAtMillis: Long
)

@Component
class JWTHelper(
    @Value("\${waic.jwt.expireAtInSeconds}")
    private val expireAtInSeconds: Int,
    @Value("\${waic.jwt.notBeforeInSeconds}")
    private val notBeforeInSeconds: Int,
    private val activityHandlerSelector: ActivityHandlerSelector
) {

    private val latestJWTMap: ConcurrentMap<String, JWTWrapper> = ConcurrentHashMap()

    fun getLatest(): String {
        val now = Instant.now().toEpochMilli()
        val activity = ThreadContextUtils.getActivity()

        val jWTWrapper = latestJWTMap[activity]
        if (jWTWrapper != null && jWTWrapper.expireAtMillis > now + SAFETY_MARGIN_MS) {
            return jWTWrapper.jwt
        }

        synchronized(this) {
            val refreshedNow = Instant.now().toEpochMilli()
            if (latestJWTMap[activity] == null
                || latestJWTMap[activity]!!.expireAtMillis <= refreshedNow + SAFETY_MARGIN_MS
            ) {
                val (newJwt, newExpireAt) = generateJWT()
                latestJWTMap[activity] = JWTWrapper(newJwt, newExpireAt)
            }
            return latestJWTMap[activity]!!.jwt
        }
    }

    private fun generateJWT(): Pair<String, Long> {
        val currentMillis = Instant.now().toEpochMilli()

        val expireAt = currentMillis + expireAtInSeconds * THOUSAND
        val notBeforeAt = currentMillis + notBeforeInSeconds * THOUSAND

        val aksk = activityHandlerSelector.selectActivityHandler().getAksk()
        log.debug("AccessKey to use: ${aksk.first}")
        val algorithm = Algorithm.HMAC256(aksk.second)
        val jwt = JWT.create()
            .withIssuer(aksk.first)
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