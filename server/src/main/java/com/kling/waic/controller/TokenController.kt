package com.kling.waic.controller

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entity.Result
import com.kling.waic.entity.Token
import com.kling.waic.helper.TokenHelper
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("/tokens")
class TokenController @Autowired constructor (
    private val tokenHelper: TokenHelper
) {
    @GetMapping("latest")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getLatestToken(): Result<Token> {
        return Result(tokenHelper.getLatest())
    }

    @GetMapping("{name}")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getToken(@PathVariable name: String): Result<Token> {
        return Result(tokenHelper.getByName(name))
    }
}