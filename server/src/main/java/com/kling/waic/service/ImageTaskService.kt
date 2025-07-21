package com.kling.waic.service

import com.google.errorprone.annotations.concurrent.LazyInit
import com.kling.waic.entity.Printing
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
import com.kling.waic.helper.CastingHelper
import com.kling.waic.helper.FaceCropper
import com.kling.waic.helper.ImageProcessHelper
import com.kling.waic.helper.PrintingHelper
import com.kling.waic.repository.CodeGenerateRepository
import com.kling.waic.helper.AESCipherHelper
import com.kling.waic.utils.IdUtils
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import redis.clients.jedis.Jedis
import java.io.File
import java.time.Instant
import javax.imageio.ImageIO

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
    private val sudokuServerDomain: String,
    @Value("\${waic.crop-image-with-opencv}")
    private val cropImageWithOpenCV: Boolean,
    @Value("\${spring.mvc.servlet.path}")
    private val servletPath: String,
    private val printingHelper: PrintingHelper,
    private val castingHelper: CastingHelper,
    private val aesCipherHelper: AESCipherHelper
) : TaskService {

    @Autowired(required = false)
    @LazyInit
    private var faceCropper: FaceCropper? = null

    companion object {
        const val TASK_N: Int = 9
    }

    override suspend fun createTask(type: TaskType, file: MultipartFile): Task {
        val taskName = codeGenerateRepository.nextCode(type)
        log.info("Generated task name: $taskName for type: $type")

        val inputImage = imageProcessHelper.multipartFileToBufferedImage(file)
        log.info("Input image size: ${inputImage.width}x${inputImage.height}")

        val requestImage = if (cropImageWithOpenCV && faceCropper != null) {
            log.debug("OpenCV face cropping is enabled, processing image with face detection")
            faceCropper!!.cropFaceToAspectRatio(inputImage, taskName)
        } else {
            log.info("OpenCV face cropping is disabled or FaceCropper not available, using original image")
            inputImage
        }

        val requestFilename = "request-${taskName}.jpg"
        val requestPath = "$sudokuImagesDir/$requestFilename"
        val requestFile = File(requestPath)
        ImageIO.write(requestImage, "jpg", requestFile)
        log.info("Saved input image to $requestPath, size: ${requestImage.width} x ${requestImage.height}")

        val requestImageUrl = "$sudokuServerDomain$servletPath$sudokuUrlPrefix/$requestFilename"
        val randomPrompts = styleImagePrompts.shuffled().take(TASK_N)

        // Use coroutineScope instead of runBlocking
        val taskIds = coroutineScope {
            val deferredResults = randomPrompts.map { prompt ->
                async(Dispatchers.IO) {
                    val request = CreateImageTaskRequest(
                        image = requestImageUrl,
                        prompt = prompt
                    )

                    val result = klingOpenAPIClient.createImageTask(request)
                    log.info(
                        "Create image task with image: $requestImageUrl, " +
                                "prompt: $prompt, taskId: ${result.data?.taskId ?: "null"}"
                    )
                    result.data?.taskId
                }
            }

            // await all deferred results
            val results = deferredResults.awaitAll()
            results.filterNotNull().toMutableList()
        }

        val task = Task(
            id = IdUtils.generateId(),
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

    override suspend fun queryTask(type: TaskType, name: String): Task {
        val task = ObjectMapperUtils.fromJSON(jedis.get(name), Task::class.java)
        if (task == null || task.type != type) {
            throw IllegalArgumentException("Task not found or type mismatch")
        }

        if (task.status in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
            log.info("Task ${task.name} is already finished with status: ${task.status}")
            return task // No need to query if the task is already completed
        }

        val taskIds = task.taskIds

        // Use coroutineScope instead of runBlocking
        val taskResponseMap = coroutineScope {
            val queryTaskRequests = taskIds.map { taskId ->
                async(Dispatchers.IO) {
                    val request = QueryImageTaskRequest(taskId = taskId)

                    val result = klingOpenAPIClient.queryImageTask(request)
                    log.info(
                        "Query task with result, taskId: {}, taskStatus: {}",
                        result.data?.taskId ?: "null", result.data?.taskStatus ?: "null"
                    )
                    taskId to result.data
                }
            }

            // Wait for all deferred results to complete
            val results = queryTaskRequests.awaitAll()
            mutableMapOf<String, QueryImageTaskResponse>().apply {
                results.forEach { (taskId, data) ->
                    data?.let { this[taskId] = it }
                }
            }
        }

        val summaryMap = summaryResponse(taskResponseMap)
        val overallStatus = calculateOverallStatus(summaryMap, taskIds.size)
        log.info("Task ${task.name} overallStatus: $overallStatus, " +
                "summaryInfo: ${summaryInfo(overallStatus, summaryMap, taskIds.size)}")

        if (overallStatus == task.status) {
            log.debug("Task ${task.name} status has not changed, returning existing task")
            return task // No status change, return existing task
        }

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
        log.debug("Set final task in Redis with name: ${finalTask.name}, value: $finalValue")

        val casting = castingHelper.addToCastingQueue(finalTask)
        log.info("Added task ${finalTask.name} to casting queue, casting: $casting")

        return finalTask
    }

    private suspend fun generateSudokuImageUrl(
        task: Task,
        taskResponseMap: Map<String, QueryImageTaskResponse>
    ): String {
        val imageUrls: List<String> = taskResponseMap.values
            .flatMap { it.taskResult.images ?: emptyList() }
            .map { it.url }

        val encodedImageName = aesCipherHelper.encrypt("sudoku-${task.name}.jpg")
        val outputPath = "$sudokuImagesDir/$encodedImageName"
        imageProcessHelper.downloadAndCreateSudoku(
            task,
            imageUrls,
            outputPath
        )
        return "$sudokuServerDomain$servletPath$sudokuUrlPrefix/$encodedImageName"
    }

    private fun summaryResponse(taskResponseMap: Map<String, QueryImageTaskResponse>):
            MutableMap<KlingOpenAPITaskStatus, MutableList<String>> {
        val summaryMap = mutableMapOf<KlingOpenAPITaskStatus, MutableList<String>>()
        taskResponseMap.forEach { taskId, response ->
            summaryMap.computeIfAbsent(response.taskStatus) { mutableListOf() }.add(taskId)
        }
        return summaryMap
    }

    private fun calculateOverallStatus(
        summaryMap: MutableMap<KlingOpenAPITaskStatus, MutableList<String>>,
        totalCount: Int
    ): TaskStatus {
        return when {
            (summaryMap[KlingOpenAPITaskStatus.submitted]?.size
                ?: 0) == totalCount -> TaskStatus.SUBMITTED

            (summaryMap[KlingOpenAPITaskStatus.succeed]?.size
                ?: 0) == totalCount -> TaskStatus.SUCCEED

            !summaryMap[KlingOpenAPITaskStatus.failed].isNullOrEmpty() -> TaskStatus.FAILED
            else -> TaskStatus.PROCESSING
        }
    }

    private fun summaryInfo(
        overallStatus: TaskStatus,
        summaryMap: MutableMap<KlingOpenAPITaskStatus, MutableList<String>>,
        totalCount: Int
    ): String {
        return when (overallStatus) {
            TaskStatus.SUBMITTED -> "All tasks are submitted: $totalCount."
            TaskStatus.SUCCEED -> "All tasks succeeded: $totalCount."
            TaskStatus.FAILED -> "Some tasks failed: " +
                    "${summaryMap[KlingOpenAPITaskStatus.failed]?.size ?: 0} / $totalCount," +
                    "failed taskIds: ${summaryMap[KlingOpenAPITaskStatus.failed]?.joinToString(", ") ?: "none"}."

            TaskStatus.PROCESSING -> "Tasks are still processing, " +
                    "submitted: ${summaryMap[KlingOpenAPITaskStatus.submitted]?.size ?: 0} / $totalCount, " +
                    "processing: ${summaryMap[KlingOpenAPITaskStatus.processing]?.size ?: 0} / $totalCount, " +
                    "succeed: ${summaryMap[KlingOpenAPITaskStatus.succeed]?.size ?: 0} / $totalCount."
        }
    }

    override suspend fun printTask(
        type: TaskType,
        name: String,
        fromConsole: Boolean
    ): Printing {
        val task = ObjectMapperUtils.fromJSON(jedis.get(name), Task::class.java)
        if (task == null || task.type != type) {
            throw IllegalArgumentException("Task not found or type mismatch")
        }

        return printingHelper.addTaskToPrintingQueue(task, fromConsole)
    }
}