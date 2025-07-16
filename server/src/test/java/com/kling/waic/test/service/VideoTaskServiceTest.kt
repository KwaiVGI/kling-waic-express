package com.kling.waic.test.service

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
        val type = TaskType.VIDEO_EFFECT
        val file = MockMultipartFile(
            "test_girl.png",
            FileUtils.readBytesFromResources("test_girl.png")
        )

        CoroutineUtils.runSuspend {
            var task = videoTaskService.createTask(type, file)

            while (task.status !in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
                delay(1000) // Use delay instead of sleep
                task = videoTaskService.queryTask(type, task.name)
            }
            assert(task.status == TaskStatus.SUCCEED) { "Task failed with status: ${task.status}" }
            println(task)
        }
    }
}