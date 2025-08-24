package com.kling.waic.api.repository

import SpringBaseTest
import com.kling.waic.component.entity.TaskType
import com.kling.waic.component.helper.AdminConfigHelper
import com.kling.waic.component.helper.TokenHelper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TokenRepositoryTest : SpringBaseTest() {

    @Autowired
    private lateinit var tokenHelper: TokenHelper
    @Autowired
    private lateinit var adminConfigHelper: AdminConfigHelper

    @Test
    fun testGetLatest() {
        val adminConfig = adminConfigHelper.getAdminConfig()
        tokenHelper.getLatest(TaskType.VIDEO_EFFECT, adminConfig).also {
            println("Latest token: $it")
        }
    }
}