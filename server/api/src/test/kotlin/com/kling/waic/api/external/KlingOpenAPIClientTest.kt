package com.kling.waic.api.external

import SpringBaseTest
import com.google.common.base.Stopwatch
import com.kling.waic.component.external.KlingOpenAPIClient
import com.kling.waic.component.external.model.CreateImageTaskRequest
import com.kling.waic.component.external.model.KlingOpenAPITaskStatus
import com.kling.waic.component.external.model.QueryImageTaskRequest
import com.kling.waic.component.external.model.QueryImageTaskResponse
import com.kling.waic.component.utils.CoroutineUtils
import com.kling.waic.component.utils.FileUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class KlingOpenAPIClientTest : SpringBaseTest() {

    @Autowired
    private lateinit var klingOpenAPIClient: KlingOpenAPIClient

    @Autowired
    private lateinit var styleImagePrompts: List<String>

    @Test
    fun testCreateImage() {
        val imageBase64 = FileUtils.convertImageToBase64(
            "test_girl.png"
        )
        val createImageTaskRequest = CreateImageTaskRequest(
            image = imageBase64,
//            prompt = "变成可图创意像素风1风格，色调清新自然，细节丰富"
//            prompt = "变成超扁平风格，高级感"
            prompt = "将图片转换成乐高风格"
        )

        CoroutineUtils.runSuspend {
            val response = klingOpenAPIClient.createImageTask(createImageTaskRequest)
            assertEquals(response.code, 0)

            val createResponse = response.data!!
            var taskStatus = createResponse.taskStatus

            val stopwatch = Stopwatch.createStarted()
            var finalResponse: QueryImageTaskResponse? = null
            while (taskStatus in setOf(
                    KlingOpenAPITaskStatus.submitted,
                    KlingOpenAPITaskStatus.processing
                )
            ) {
                Thread.sleep(1000)

                finalResponse = klingOpenAPIClient.queryImageTask(
                    queryImageTaskRequest = QueryImageTaskRequest(taskId = createResponse.taskId)
                ).data!!
                log.info("Current task status: ${finalResponse.taskStatus}")
                taskStatus = finalResponse.taskStatus
            }

            assertEquals(finalResponse!!.taskStatus, KlingOpenAPITaskStatus.succeed)
            log.info("Final task status: ${finalResponse.taskStatus}, taskResult: ${finalResponse.taskResult}, time taken: $stopwatch")
        }
    }
}