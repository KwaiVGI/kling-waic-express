package com.kling.waic.api.service

import SpringBaseTest
import com.kling.waic.component.entity.Locale
import com.kling.waic.component.entity.TaskStatus
import com.kling.waic.component.entity.TaskType
import com.kling.waic.component.service.TaskService
import com.kling.waic.component.utils.CoroutineUtils
import com.kling.waic.component.utils.FileUtils
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class ImageTaskServiceTest : SpringBaseTest() {

    @Autowired
    private lateinit var imageTaskService: TaskService

    @Test
    fun testUploadImage() {
        val filename = "origin_17530997642371778_IMG_3654.JPG"
        val type = TaskType.STYLED_IMAGE
        val file = MockMultipartFile(
            filename,
            filename,
            "image/jpeg",
            FileUtils.readBytesFromResources(filename)
        )
        val url = imageTaskService.uploadImage(type, file)

        assert(url.isNotEmpty()) { "Uploaded image URL should not be empty" }
        println("Uploaded image URL: $url")
    }

    @Test
    fun testImageTaskService() {
        val type = TaskType.STYLED_IMAGE
        val url = "https://kling-waic.s3.cn-north-1.amazonaws.com.cn/input-images/request-No.100228.jpg"

        CoroutineUtils.runSuspend {
            var task = imageTaskService.createTask(type, url)

            while (task.status !in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
                delay(1000) // Use delay instead of sleep
                task = imageTaskService.queryTask(type, task.name, Locale.CN)
            }
            assert(task.status == TaskStatus.SUCCEED) { "Task failed with status: ${task.status}" }
            println(task)
        }
    }
}