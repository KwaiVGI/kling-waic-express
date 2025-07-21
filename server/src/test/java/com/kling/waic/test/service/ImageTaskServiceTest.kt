package com.kling.waic.test.service

import com.kling.waic.entity.Locale
import com.kling.waic.entity.TaskStatus
import com.kling.waic.entity.TaskType
import com.kling.waic.service.TaskService
import com.kling.waic.test.SpringBaseTest
import com.kling.waic.utils.CoroutineUtils
import com.kling.waic.utils.FileUtils
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class ImageTaskServiceTest : SpringBaseTest() {

    @Autowired
    private lateinit var imageTaskService: TaskService

    @Test
    fun testImageTaskService() {
        val filename = "request-No.100228.jpg"
        val type = TaskType.STYLED_IMAGE
        val file = MockMultipartFile(
            filename,
            FileUtils.readBytesFromResources(filename)
        )

        CoroutineUtils.runSuspend {
            var task = imageTaskService.createTask(type, file)

            while (task.status !in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
                delay(1000) // Use delay instead of sleep
                task = imageTaskService.queryTask(type, task.name, Locale.CN)
            }
            assert(task.status == TaskStatus.SUCCEED) { "Task failed with status: ${task.status}" }
            println(task)
        }
    }
}