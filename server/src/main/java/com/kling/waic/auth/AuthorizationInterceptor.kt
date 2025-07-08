package com.kling.waic.auth

import com.kling.waic.repositories.TokenRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
open class AuthorizationInterceptor (
    val tokenRepository: TokenRepository,
    @Value("\${waic.management.access-key}") val accessKey: String,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val annotation = (handler as HandlerMethod).getMethodAnnotation<Authorization>(Authorization::class.java)
        if (annotation == null) {
            return true
        }
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Token ")) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("No authorization data detected")
            return false
        }
        val token = authHeader.substring(6) // Remove "Token " prefix
        if (!validateToken(token, annotation.type)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("Invalid token")
            return false
        }
        return true
    }

    private fun validateToken(token: String, type: AuthorizationType): Boolean {
        return if (type == AuthorizationType.CREATE_TASK) {
            tokenRepository.validate(token)
        } else if (type == AuthorizationType.MANAGEMENT) {
            token == accessKey
        } else {
            false
        }
    }
}