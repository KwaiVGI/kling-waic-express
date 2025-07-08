package com.kling.waic.controllers

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entities.Result
import com.kling.waic.entities.Token
import com.kling.waic.repositories.TokenRepository
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("/tokens")
class TokenController @Autowired constructor (
    private val tokenRepository: TokenRepository
) {
    @GetMapping("latest")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getLatestToken(): Result<Token> {
        return Result(tokenRepository.getLatest())
    }

    @GetMapping("{name}")
    fun getToken(@PathVariable name: String): Result<Token> {
        return Result(tokenRepository.getByName(name))
    }
}