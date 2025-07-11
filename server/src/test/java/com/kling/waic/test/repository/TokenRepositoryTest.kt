package com.kling.waic.test.repository

import com.kling.waic.repositories.TokenRepository
import com.kling.waic.test.SpringBaseTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TokenRepositoryTest : SpringBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Test
    fun testGetLatest() {
        tokenRepository.getLatest().also {
            println("Latest token: $it")
        }
    }
}