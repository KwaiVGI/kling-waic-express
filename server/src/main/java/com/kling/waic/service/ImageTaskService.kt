package com.kling.waic.service

import com.kling.waic.entities.Task
import com.kling.waic.entities.TaskOutput
import com.kling.waic.entities.TaskOutputType
import com.kling.waic.entities.TaskStatus
import com.kling.waic.entities.TaskType
import com.kling.waic.external.KlingOpenAPIClient
import com.kling.waic.external.model.CreateImageTaskRequest
import com.kling.waic.external.model.KlingOpenAPITaskStatus
import com.kling.waic.external.model.QueryImageTaskRequest
import com.kling.waic.external.model.QueryImageTaskResponse
import com.kling.waic.helper.ImageProcessHelper
import com.kling.waic.repositories.CodeGenerateRepository
import com.kling.waic.utils.FileUtils
import com.kling.waic.utils.ObjectMapperUtils
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import redis.clients.jedis.Jedis
import java.time.Instant
import java.util.*

@Service
class ImageTaskService(
    private val klingOpenAPIClient: KlingOpenAPIClient,
    private val styleImagePrompts: List<String>,
    private val codeGenerateRepository: CodeGenerateRepository,
    private val jedis: Jedis,
    private val imageProcessHelper: ImageProcessHelper,
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
        val imageBase64 = FileUtils.convertImageToBase64(file)
        val randomPrompts = styleImagePrompts.shuffled().take(TASK_N)
        val taskIds = mutableListOf<String>()
        for (prompt in randomPrompts) {
            val request = CreateImageTaskRequest(
                image = imageBase64,
                prompt = prompt
            )
            val result = klingOpenAPIClient.createImageTask(request)
            taskIds.add(result.data!!.taskId)
        }

        val task = Task(
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
            name = codeGenerateRepository.nextCode(type),
            taskIds = taskIds,
            status = TaskStatus.SUBMITTED,
            type = type,
            filename = file.name,
            createTime = Instant.now(),
            updateTime = Instant.now(),
        )
        jedis.set(task.name, ObjectMapperUtils.toJSON(task))
        return task
    }

    override fun queryTask(type: TaskType, name: String): Task {
        val task = ObjectMapperUtils.fromJSON(jedis.get(name), Task::class.java)
        if (task == null || task.type != type) {
            throw IllegalArgumentException("Task not found or type mismatch")
        }

        val taskIds = task.taskIds
        val taskResponseMap = mutableMapOf<String, QueryImageTaskResponse>()
        for (taskId in taskIds) {
            val request = QueryImageTaskRequest(taskId = taskId)
            val result = klingOpenAPIClient.queryImageTask(request)
            taskResponseMap.put(taskId, result.data!!)
        }

        val overallStatus = calculateStatus(taskResponseMap)
        val newTask = task.copy(
            status = overallStatus,
            updateTime = Instant.now(),
        )
        jedis.set(task.name, ObjectMapperUtils.toJSON(newTask))

        if (overallStatus != TaskStatus.SUCCEED) {
            return newTask
        }

        val url = generateSudokuImageUrl(newTask, taskResponseMap)

        val finalTask = newTask.copy(
            outputs = TaskOutput(
                type = TaskOutputType.IMAGE,
                url = url
            ),
            updateTime = Instant.now()
        )
        jedis.set(task.name, ObjectMapperUtils.toJSON(newTask))
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
                imageUrls,
                outputPath
            )
        }
        return "$sudokuServerDomain/$sudokuUrlPrefix/sudoku-${task.name}.jpg"
    }

    private fun calculateStatus(taskResponseMap: Map<String, QueryImageTaskResponse>): TaskStatus {
        return when {
            taskResponseMap.values.all { it.taskStatus == KlingOpenAPITaskStatus.submitted } -> TaskStatus.SUBMITTED
            taskResponseMap.values.all { it.taskStatus == KlingOpenAPITaskStatus.succeed } -> TaskStatus.SUCCEED
            taskResponseMap.values.any { it.taskStatus == KlingOpenAPITaskStatus.failed } -> TaskStatus.FAILED
            else -> TaskStatus.PROCESSING
        }
    }
}