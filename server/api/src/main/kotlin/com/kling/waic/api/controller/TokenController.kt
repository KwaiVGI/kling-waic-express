package com.kling.waic.api.controller

import com.kling.waic.component.auth.Authorization
import com.kling.waic.component.auth.AuthorizationType
import com.kling.waic.component.entity.Result
import com.kling.waic.component.entity.TaskType
import com.kling.waic.component.entity.Token
import com.kling.waic.component.helper.AdminConfigHelper
import com.kling.waic.component.helper.TokenHelper
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("/tokens")
class TokenController @Autowired constructor (
    private val tokenHelper: TokenHelper,
    private val adminConfigHelper: AdminConfigHelper
) {

    @GetMapping("{type}/latest")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getLatestTypeToken(@PathVariable type: TaskType): Result<Token> {
        val adminConfig = adminConfigHelper.getAdminConfig()
        return Result(tokenHelper.getLatest(type, adminConfig))
    }

    @GetMapping("{name}")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getToken(@PathVariable name: String): Result<Token> {
        return Result(tokenHelper.getByName(name))
    }
}