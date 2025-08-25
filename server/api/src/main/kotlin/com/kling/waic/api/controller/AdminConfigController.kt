package com.kling.waic.api.controller

import com.kling.waic.component.auth.Authorization
import com.kling.waic.component.auth.AuthorizationType
import com.kling.waic.component.entity.AdminConfig
import com.kling.waic.component.entity.Result
import com.kling.waic.component.helper.AdminConfigHelper
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/config")
class AdminConfigController(
    private val adminConfigHelper: AdminConfigHelper
) {

    @GetMapping("fetch")
    fun fetch(): Result<AdminConfig> {
        val adminConfig = adminConfigHelper.getAdminConfig()
        return Result(adminConfig)
    }

    @PostMapping("save")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun operate(@RequestBody input: AdminConfig): Result<String> {
        val result = adminConfigHelper.saveAdminConfig(adminConfig = input)
        return Result(result)
    }
}