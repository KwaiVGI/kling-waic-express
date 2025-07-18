package com.kling.waic.auth

import com.kling.waic.repository.TokenRepository
import com.kling.waic.utils.Slf4j.Companion.log
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
open class AuthorizationInterceptor (
    private val tokenRepository: TokenRepository,
    @Value("\${waic.management.token}")
    private val waicManagementToken: String
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
//        if ("OPTIONS".equals(request.method, ignoreCase = true)) {
//            log.info("CORS preflight request allowed: ${request.requestURI}")
//            return true
//        }
        
        if (handler !is HandlerMethod) {
            log.info("AuthorizationInterceptor preHandle called for request: ${request.requestURI}," +
                    " method: ${request.method}, handler class: ${handler.javaClass}")
            return true
        }
        val annotation = handler.getMethodAnnotation(Authorization::class.java)
        if (annotation == null) {
            return true
        }
        
        log.info("Authorization required for: ${request.requestURI}, type: ${annotation.type}")
        
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Token ")) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("No authorization data detected")
            return false
        }
        
        // Check token
        val token = authHeader.substring(6) // Remove "Token " prefix
        if (!validateToken(token, annotation.type)) {
            log.warn("Authorization failed - invalid token for: ${request.requestURI}")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("Invalid token")
            return false
        }
        
        log.info("Authorization successful for: ${request.requestURI}")
        return true
    }

    private fun validateToken(token: String, type: AuthorizationType): Boolean {
        return if (type == AuthorizationType.CREATE_TASK) {
            tokenRepository.validate(token)
        } else if (type == AuthorizationType.MANAGEMENT) {
            token == waicManagementToken
        } else {
            false
        }
    }
}