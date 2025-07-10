package com.kling.waic.test.external

import com.google.common.base.Stopwatch
import com.kling.waic.external.KlingOpenAPIClient
import com.kling.waic.external.model.CreateImageTaskRequest
import com.kling.waic.external.model.QueryImageTaskRequest
import com.kling.waic.external.model.QueryImageTaskResponse
import com.kling.waic.test.SpringBaseTest
import com.kling.waic.utils.FileUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class KlingOpenAPIClientTest : SpringBaseTest() {

    @Autowired
    private lateinit var klingOpenAPIClient: KlingOpenAPIClient

    @Test
    fun testCreateImage() {
        val imageBase64 = FileUtils.covertImageToBase64(
            "test_girl.png"
        )
        val createImageTaskRequest = CreateImageTaskRequest(
            image = imageBase64
        )
        val response = klingOpenAPIClient.createImageTask(createImageTaskRequest)
        assertEquals(response.code, 0)

        val createResponse = response.data!!
        var taskStatus = createResponse.taskStatus

        val stopwatch = Stopwatch.createStarted()
        var finalResponse: QueryImageTaskResponse? = null
        while (taskStatus in setOf("submitted", "processing")) {
            Thread.sleep(1000)

            finalResponse = klingOpenAPIClient.queryImageTask(
                queryImageTaskRequest = QueryImageTaskRequest(taskId = createResponse.taskId)
            ).data!!
            log.info("Current task status: ${finalResponse.taskStatus}")
            taskStatus = finalResponse.taskStatus
        }

        assertEquals(finalResponse!!.taskStatus, "succeed")
        log.info("Final task status: ${finalResponse.taskStatus}, taskResult: ${finalResponse.taskResult}, time taken: $stopwatch")
    }
}