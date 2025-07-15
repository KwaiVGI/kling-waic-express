package com.kling.waic.test.service

import com.kling.waic.entity.TaskStatus
import com.kling.waic.entity.TaskType
import com.kling.waic.service.TaskService
import com.kling.waic.test.SpringBaseTest
import com.kling.waic.utils.FileUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import java.lang.Thread.sleep

class TaskServiceTest : SpringBaseTest() {

    @Autowired
    private lateinit var imageTaskService: TaskService

    @Test
    fun testImageTaskService() {
        val type = TaskType.STYLED_IMAGE
        val file = MockMultipartFile(
            "test_girl.png",
            FileUtils.readBytesFromResources("test_girl.png")
        )
        var task = imageTaskService.createTask(type, file)

        while (task.status !in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
            sleep(1000)
            task = imageTaskService.queryTask(type, task.name)
        }
        assert(task.status == TaskStatus.SUCCEED) { "Task failed with status: ${task.status}" }
        println(task)
    }
}