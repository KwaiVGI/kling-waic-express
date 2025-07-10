package com.kling.waic.test.external

import com.kling.waic.external.KlingOpenAPIClient
import com.kling.waic.external.model.ImageTaskRequest
import com.kling.waic.test.SpringBaseTest
import com.kling.waic.utils.FileUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class KlingOpenAPIClientTest : SpringBaseTest() {

    @Autowired
    private lateinit var klingOpenAPIClient: KlingOpenAPIClient

    @Test
    fun testCreateImageTask() {
        val imageBase64 = FileUtils.covertImageToBase64(
            "test_girl.png"
        )
        val imageTaskRequest = ImageTaskRequest(
            image = imageBase64
        )
        val response = klingOpenAPIClient.createImageTask(imageTaskRequest)
        println(response)
        assertEquals(response.code, 0)
    }
}