package com.kling.waic.test.repository

import com.kling.waic.helper.TokenHelper
import com.kling.waic.test.SpringBaseTest
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