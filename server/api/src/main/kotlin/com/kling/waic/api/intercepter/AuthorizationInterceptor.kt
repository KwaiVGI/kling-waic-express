package com.kling.waic.api.intercepter

import com.kling.waic.component.auth.Authorization
import com.kling.waic.component.auth.AuthorizationType
import com.kling.waic.component.entity.TokenMapConfig
import com.kling.waic.component.helper.TokenHelper
import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.component.utils.ThreadContextUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
open class AuthorizationInterceptor (
    private val tokenHelper: TokenHelper,
    @Value("\${WAIC_MANAGEMENT_TOKEN}")
    private val waicManagementToken: String,
    private val tokenMapConfig: TokenMapConfig
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
//        if ("OPTIONS".equals(request.method, ignoreCase = true)) {
//            log.info("CORS preflight request allowed: ${request.requestURI}")
//            return true
//        }

        if (handler !is HandlerMethod) {
            log.debug(
                "AuthorizationInterceptor preHandle called for request: {}, method: {}, handler class: {}",
                request.requestURI,
                request.method,
                handler.javaClass
            )
            return true
        }
        val annotation = handler.getMethodAnnotation(Authorization::class.java)
        if (annotation == null) {
            return true
        }

        log.debug("Authorization required for: {}, type: {}", request.requestURI, annotation.type)

        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Token ")) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("No authorization data detected")
            return false
        }

        // Check token
        val activity = request.getHeader("Activity") ?: ""
        val token = authHeader.substring(6) // Remove "Token " prefix
        if (!validateToken(activity, token, annotation.type)) {
            log.warn(
                "Authorization failed - invalid token: $token with type: ${annotation.type} " +
                        "for activity: $activity, uri: ${request.requestURI}"
            )
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("Invalid token")
            return false
        }

        ThreadContextUtils.putActivity(activity)
        log.debug("Authorization successful for: ${request.requestURI}")
        return true
    }

    private fun validateToken(
        activity: String,
        token: String,
        type: AuthorizationType
    ): Boolean {
        return if (type == AuthorizationType.CREATE_TASK) {
            tokenHelper.validate(activity, token)
        } else if (type == AuthorizationType.MANAGEMENT) {
            val manageToken = tokenMapConfig.map[activity] ?: waicManagementToken
            token == manageToken
        } else {
            false
        }
    }
}