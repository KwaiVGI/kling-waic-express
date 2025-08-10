package com.kling.waic.test.repository

import SpringBaseTest
import com.kling.waic.component.helper.TokenHelper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TokenRepositoryTest : SpringBaseTest() {

    @Autowired
    private lateinit var tokenHelper: TokenHelper

    @Test
    fun testGetLatest() {
        tokenHelper.getLatest().also {
            println("Latest token: $it")
        }
    }
}