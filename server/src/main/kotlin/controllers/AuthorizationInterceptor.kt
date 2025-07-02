package com.klingai.express.controllers

import com.klingai.express.repositories.ConfigurationRepository
import com.klingai.express.repositories.TokenRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthorizationInterceptor @Autowired constructor (
    val configuration: ConfigurationRepository,
    val tokenRepository: TokenRepository
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
        if (type == AuthorizationType.CREATE_TASK) {
            return this.tokenRepository.validate(token)
        } else if (type == AuthorizationType.MANAGEMENT) {
            return token == this.configuration.managementAccessKey
        } else {
            return false
        }
    }
}