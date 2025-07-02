package com.klingai.express.controllers

import com.klingai.express.entities.Result
import com.klingai.express.entities.Token
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import com.klingai.express.repositories.TokenRepository
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