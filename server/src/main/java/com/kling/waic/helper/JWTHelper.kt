package com.kling.waic.helper

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.stereotype.Component
import java.util.*


@Component
class JWTHelper (
    private val waicOpenApiAccessKey: String,
    private val waicOpenApiSecretKey: String
) {

    fun generateJWT(): String? {
            val expiredAt = Date(System.currentTimeMillis() + 1800 * 1000) // 有效时间，此处示例代表当前时间+1800s(30min)
            val notBefore = Date(System.currentTimeMillis() - 5 * 1000) //开始生效的时间，此处示例代表当前时间-5秒
            val algo: Algorithm? = Algorithm.HMAC256(waicOpenApiSecretKey)
        val header: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        header.put("alg", "HS256")
            return JWT.create()

                .withIssuer(waicOpenApiAccessKey)
                .withHeader(header)
                .withExpiresAt(expiredAt)
                .withNotBefore(notBefore)
                .sign(algo)
    }
}