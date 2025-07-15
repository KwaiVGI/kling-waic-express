package com.kling.waic.service

import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskOutput
import com.kling.waic.entity.TaskOutputType
import com.kling.waic.entity.TaskStatus
import com.kling.waic.entity.TaskType
import com.kling.waic.external.KlingOpenAPIClient
import com.kling.waic.external.model.CreateImageTaskRequest
import com.kling.waic.external.model.KlingOpenAPITaskStatus
import com.kling.waic.external.model.QueryImageTaskRequest
import com.kling.waic.external.model.QueryImageTaskResponse
import com.kling.waic.helper.FaceCropper
import com.kling.waic.helper.ImageProcessHelper
import com.kling.waic.repository.CodeGenerateRepository
import com.kling.waic.utils.FileUtils
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import redis.clients.jedis.Jedis
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO

@Service
class ImageTaskService(
    private val klingOpenAPIClient: KlingOpenAPIClient,
    private val styleImagePrompts: List<String>,
    private val codeGenerateRepository: CodeGenerateRepository,
    private val jedis: Jedis,
    private val imageProcessHelper: ImageProcessHelper,
    private val faceCropper: FaceCropper,
    @Value("\${waic.sudoku.images-dir}")
    private val sudokuImagesDir: String,
    @Value("\${waic.sudoku.url-prefix}")
    private val sudokuUrlPrefix: String,
    @Value("\${waic.sudoku.server-domain}")
    private val sudokuServerDomain: String
) : TaskService {

    companion object {
        const val TASK_N: Int = 9
    }

    override fun createTask(type: TaskType, file: MultipartFile): Task {
        val taskName = codeGenerateRepository.nextCode(type)
        log.info("Generated task name: $taskName for type: $type")

        val inputImage = multipartFileToBufferedImage(file)
        log.info("Input image size: ${inputImage.width}x${inputImage.height}")

        val outputImage = faceCropper.cropFaceToAspectRatio(inputImage, taskName)
        log.info("Output image size: ${outputImage.width}x${outputImage.height}")

        val imageBase64 = FileUtils.convertImageToBase64(outputImage)

        val randomPrompts = styleImagePrompts.shuffled().take(TASK_N)
        val taskIds = mutableListOf<String>()
        for (prompt in randomPrompts) {
            val request = CreateImageTaskRequest(
                image = imageBase64,
                prompt = prompt
            )
            val result = klingOpenAPIClient.createImageTask(request)
            log.info("Create image task with prompt: $prompt, taskId: ${result.data?.taskId ?: "null"}")
            taskIds.add(result.data!!.taskId)
        }

        val task = Task(
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
            name = taskName,
            taskIds = taskIds,
            status = TaskStatus.SUBMITTED,
            type = type,
            filename = file.name,
            createTime = Instant.now(),
            updateTime = Instant.now(),
        )

        val newValue = ObjectMapperUtils.toJSON(task)
        jedis.set(task.name, newValue)
        log.info("Set task in Redis with name: ${task.name}, value: $newValue")
        return task
    }

    override fun queryTask(type: TaskType, name: String): Task {
        val task = ObjectMapperUtils.fromJSON(jedis.get(name), Task::class.java)
        if (task == null || task.type != type) {
            throw IllegalArgumentException("Task not found or type mismatch")
        }

        if (task.status in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
            log.info("Task ${task.name} is already finished with status: ${task.status}")
            return task // No need to query if the task is already completed
        }

        val taskIds = task.taskIds
        val taskResponseMap = mutableMapOf<String, QueryImageTaskResponse>()
        for (taskId in taskIds) {
            val request = QueryImageTaskRequest(taskId = taskId)
            val result = klingOpenAPIClient.queryImageTask(request)
            log.info("Query task with result, taskId: {}, taskStatus: {}",
                result.data?.taskId ?: "null", result.data?.taskStatus ?: "null")
            taskResponseMap.put(taskId, result.data!!)
        }

        val overallStatus = calculateStatus(taskResponseMap)
        log.info("Task ${task.name} overallStatus: $overallStatus")

        val newTask = task.copy(
            status = overallStatus,
            updateTime = Instant.now(),
        )
        val newValue = ObjectMapperUtils.toJSON(newTask)
        jedis.set(task.name, newValue)
        log.info("Set updated task in Redis with name: ${newTask.name}, value: $newValue")

        if (overallStatus != TaskStatus.SUCCEED) {
            return newTask
        }

        log.info("Generating Sudoku image URL for task: ${newTask.name}")
        val url = generateSudokuImageUrl(newTask, taskResponseMap)

        val finalTask = newTask.copy(
            outputs = TaskOutput(
                type = TaskOutputType.IMAGE,
                url = url
            ),
            updateTime = Instant.now()
        )

        val finalValue = ObjectMapperUtils.toJSON(finalTask)
        jedis.set(task.name, finalValue)
        log.info("Set final task in Redis with name: ${finalTask.name}, value: $finalValue")
        return finalTask
    }

    private fun generateSudokuImageUrl(task: Task,
                                       taskResponseMap: Map<String, QueryImageTaskResponse>): String {
        val imageUrls: List<String> = taskResponseMap.values
            .flatMap { it.taskResult.images ?: emptyList() }
            .map { it.url }

        val outputPath = "$sudokuImagesDir/sudoku-${task.name}.jpg"
        runBlocking {
            imageProcessHelper.downloadAndCreateSudoku(
                task,
                imageUrls,
                outputPath
            )
        }
        return "$sudokuServerDomain/api$sudokuUrlPrefix/sudoku-${task.name}.jpg"
    }

    private fun calculateStatus(taskResponseMap: Map<String, QueryImageTaskResponse>): TaskStatus {
        return when {
            taskResponseMap.values.all { it.taskStatus == KlingOpenAPITaskStatus.submitted } -> TaskStatus.SUBMITTED
            taskResponseMap.values.all { it.taskStatus == KlingOpenAPITaskStatus.succeed } -> TaskStatus.SUCCEED
            taskResponseMap.values.any { it.taskStatus == KlingOpenAPITaskStatus.failed } -> TaskStatus.FAILED
            else -> TaskStatus.PROCESSING
        }
    }

    fun multipartFileToBufferedImage(file: MultipartFile): BufferedImage {
        file.inputStream.use { inputStream ->
            return ImageIO.read(inputStream)
        }
    }
}