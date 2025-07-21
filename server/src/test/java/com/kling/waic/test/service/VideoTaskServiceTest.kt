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

class VideoTaskServiceTest : SpringBaseTest() {

    @Autowired
    private lateinit var videoTaskService: TaskService

    @Test
    fun testVideoTaskService() {
        val filename = "origin_17530997642371778_IMG_3654.JPG"
        val type = TaskType.VIDEO_EFFECT
        val file = MockMultipartFile(
            filename,
            FileUtils.readBytesFromResources(filename)
        )

        CoroutineUtils.runSuspend {
            var task = videoTaskService.createTask(type, file)

            while (task.status !in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
                delay(1000) // Use delay instead of sleep
                task = videoTaskService.queryTask(type, task.name, Locale.CN)
            }
            assert(task.status == TaskStatus.SUCCEED) { "Task failed with status: ${task.status}" }
            println(task)
        }
    }
}