package com.kling.waic.service

import com.kling.waic.entity.Printing
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskInput
import com.kling.waic.entity.TaskOutput
import com.kling.waic.entity.TaskOutputType
import com.kling.waic.entity.TaskStatus
import com.kling.waic.entity.TaskType
import com.kling.waic.exception.KlingOpenAPIException
import com.kling.waic.external.KlingOpenAPIClient
import com.kling.waic.external.model.CreateVideoTaskInput
import com.kling.waic.external.model.CreateVideoTaskRequest
import com.kling.waic.external.model.KlingOpenAPITaskStatus
import com.kling.waic.external.model.QueryVideoTaskRequest
import com.kling.waic.helper.CastingHelper
import com.kling.waic.helper.ImageProcessHelper
import com.kling.waic.helper.S3Helper
import com.kling.waic.repository.CodeGenerateRepository
import com.kling.waic.utils.IdUtils
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import redis.clients.jedis.commands.JedisCommands
import java.time.Instant

@Service
class VideoTaskService(
    private val imageProcessHelper: ImageProcessHelper,
    private val videoSpecialEffects: List<String>,
    private val klingOpenAPIClient: KlingOpenAPIClient,
    private val codeGenerateRepository: CodeGenerateRepository,
    @Value("\${waic.sudoku.images-dir}")
    private val sudokuImagesDir: String,
    @Value("\${waic.sudoku.url-prefix}")
    private val sudokuUrlPrefix: String,
    @Value("\${waic.sudoku.server-domain}")
    private val sudokuServerDomain: String,
    @Value("\${waic.crop-image-with-opencv}")
    private val cropImageWithOpenCV: Boolean,
    @Value("\${spring.mvc.servlet.path}")
    private val servletPath: String,
    private val jedis: JedisCommands,
    private val castingHelper: CastingHelper,
    private val s3Helper: S3Helper,
    @param:Value("\${s3.bucket}") private val bucket: String,
) : TaskService {

    override suspend fun createTask(type: TaskType, file: MultipartFile): Task {
        val taskName = codeGenerateRepository.nextCode(type)
        log.info("Generated task name: $taskName for type: $type")

        val inputImage = imageProcessHelper.multipartFileToBufferedImage(file)
        log.info("Input image size: ${inputImage.width}x${inputImage.height}")

        val effectScene = videoSpecialEffects.random()

        val requestImage = inputImage
        val requestFilename = "request-${taskName}.jpg"
        val requestImageUrl = s3Helper.uploadBufferedImage(
            bucket,
            "request-images/$requestFilename",
            requestImage, "jpg"
        )

        val request = CreateVideoTaskRequest(
            effectScene = effectScene,
            input = CreateVideoTaskInput(
                image = requestImageUrl
            )
        )
        val result = klingOpenAPIClient.createVideoTask(request)
        if (result.code != 0) {
            throw KlingOpenAPIException(result)
        }
        log.info("Create video task with effectScene: $effectScene, taskId: ${result.data?.taskId ?: "null"}")

        val task = Task(
            id = IdUtils.generateId(),
            name = taskName,
            input = TaskInput(
                type = type,
                image = requestImageUrl,
            ),
            taskIds = listOf(result.data!!.taskId),
            status = TaskStatus.SUBMITTED,
            type = type,
            filename = file.name,
            createTime = Instant.now(),
            updateTime = Instant.now(),
        )

        val value = ObjectMapperUtils.toJSON(task)
        jedis.set(task.name, value)
        log.info("Set task in Redis with name: ${task.name}, value: $value")
        return task
    }

    override suspend fun queryTask(
        type: TaskType,
        name: String
    ): Task {
        val task = ObjectMapperUtils.fromJSON(jedis.get(name), Task::class.java)
        if (task == null || task.type != type) {
            log.error("Task type $type not found or type mismatch for task: $name, task: $task")
            throw IllegalArgumentException("Task not found or type mismatch")
        }

        if (task.status in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
            log.info("Task ${task.name} is already finished with status: ${task.status}")
            return task // No need to query if the task is already completed
        }

        val taskId = task.taskIds.first()
        val request = QueryVideoTaskRequest(taskId)
        val result = klingOpenAPIClient.queryVideoTask(request)
        if (result.code != 0) {
            throw KlingOpenAPIException(result)
        }
        log.debug(
            "Query task with result, taskId: {}, taskStatus: {}",
            taskId,
            result.data?.taskStatus ?: "null"
        )

        val taskStatus = result.data!!.taskStatus
        val convertedStatus = calculateStatus(taskStatus)
        log.info("Task ${task.name} convertedStatus: $convertedStatus")

        if (convertedStatus == task.status) {
            log.debug("Task ${task.name} status has not changed, returning existing task")
            return task // No status change, return existing task
        }

        val newTask = task.copy(
            status = convertedStatus,
            updateTime = Instant.now(),
        )
        val newValue = ObjectMapperUtils.toJSON(newTask)
        jedis.set(task.name, newValue)
        log.info("Set updated task in Redis with name: ${newTask.name}, value: $newValue")

        if (convertedStatus != TaskStatus.SUCCEED) {
            return newTask
        }

        val url = result.data.taskResult.videos!!.first().url
        val finalTask = newTask.copy(
            outputs = TaskOutput(
                type = TaskOutputType.VIDEO,
                url = url
            ),
            updateTime = Instant.now()
        )

        val finalValue = ObjectMapperUtils.toJSON(finalTask)
        jedis.set(task.name, finalValue)
        log.debug("Set final task in Redis with name: ${finalTask.name}, value: $finalValue")

        val casting = castingHelper.addToCastingQueue(finalTask)
        log.info("Added task ${finalTask.name} to casting queue, casting: $casting")

        return finalTask
    }

    private fun calculateStatus(taskStatus: KlingOpenAPITaskStatus): TaskStatus {
        return when (taskStatus) {
            KlingOpenAPITaskStatus.submitted -> TaskStatus.SUBMITTED
            KlingOpenAPITaskStatus.processing -> TaskStatus.PROCESSING
            KlingOpenAPITaskStatus.succeed -> TaskStatus.SUCCEED
            KlingOpenAPITaskStatus.failed -> TaskStatus.FAILED
        }
    }

    override suspend fun printTask(
        type: TaskType,
        name: String,
        fromConsole: Boolean
    ): Printing {
        throw UnsupportedOperationException("Print video is not implemented yet")
    }
}