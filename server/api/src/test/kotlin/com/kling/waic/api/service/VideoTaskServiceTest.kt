package service

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

        val url = "https://kling-waic.s3.cn-north-1.amazonaws.com.cn/input-images/request-No.100228.jpg"
        CoroutineUtils.runSuspend {
            var task = videoTaskService.createTask(type, url)

            while (task.status !in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
                delay(1000) // Use delay instead of sleep
                task = videoTaskService.queryTask(type, task.name, Locale.CN)
            }
            assert(task.status == TaskStatus.SUCCEED) { "Task failed with status: ${task.status}" }
            println(task)
        }
    }
}