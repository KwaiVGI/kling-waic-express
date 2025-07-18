package com.kling.waic.test.helper

import com.kling.waic.helper.AESCipherHelper
import com.kling.waic.test.SpringBaseTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class AESCipherHelperTest : SpringBaseTest() {

    @Autowired
    private lateinit var aesCipherHelper: AESCipherHelper

    @Test
    fun testCipher() {
        val rawStr = "No.100017"

        val encodedStr = aesCipherHelper.encrypt(rawStr)
        println(encodedStr)
        val decodedStr = aesCipherHelper.decrypt(encodedStr)
        println(decodedStr)

        assertEquals(rawStr, decodedStr)

        val hmacStr = aesCipherHelper.hmacSHA256(rawStr)
        println(hmacStr)
    }
}