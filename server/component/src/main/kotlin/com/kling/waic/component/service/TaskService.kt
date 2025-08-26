package com.kling.waic.component.service

import com.google.errorprone.annotations.concurrent.LazyInit
import com.kling.waic.component.helper.S3Helper
import com.kling.waic.component.repository.CodeGenerateRepository
import com.kling.waic.component.entity.Locale
import com.kling.waic.component.entity.OpenApiRecord
import com.kling.waic.component.entity.Printing
import com.kling.waic.component.entity.Task
import com.kling.waic.component.entity.TaskInput
import com.kling.waic.component.entity.TaskOutput
import com.kling.waic.component.entity.TaskOutputType
import com.kling.waic.component.entity.TaskStatus
import com.kling.waic.component.entity.TaskType
import com.kling.waic.component.external.model.QueryTaskContext
import com.kling.waic.component.helper.AESCipherHelper
import com.kling.waic.component.helper.AdminConfigHelper
import com.kling.waic.component.helper.ImageCropHelper
import com.kling.waic.component.helper.ImageProcessHelper
import com.kling.waic.component.repository.CastingRepository
import com.kling.waic.component.repository.PrintingRepository
import com.kling.waic.component.repository.TaskRepository
import com.kling.waic.component.utils.IdUtils
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.time.Instant

abstract class TaskService {
    @Autowired
    private lateinit var s3Helper: S3Helper
    @Autowired
    private lateinit var codeGenerateRepository: CodeGenerateRepository
    @Autowired
    private lateinit var taskRepository: TaskRepository
    @Autowired
    private lateinit var imageProcessHelper: ImageProcessHelper
    @Autowired
    private lateinit var castingRepository: CastingRepository
    @Autowired
    private lateinit var printingRepository: PrintingRepository
    @Value("\${S3_BUCKET_NAME:kling-waic}")
    private lateinit var bucket: String
    @Value("\${WAIC_CROP_IMAGE_WITH_OPENCV:true}")
    private lateinit var cropImageWithOpenCV: String
    @Autowired
    private lateinit var aesCipherHelper: AESCipherHelper
    @Autowired
    private lateinit var adminConfigHelper: AdminConfigHelper

    @Autowired(required = false)
    @LazyInit
    private var imageCropHelper: ImageCropHelper? = null

    fun uploadImage(type: TaskType, file: MultipartFile): String {
        val taskName = codeGenerateRepository.nextCode(type)
        val inputImage = imageProcessHelper.multipartFileToBufferedImage(file)

        val requestImage = if (cropImageWithOpenCV.toBoolean() && imageCropHelper != null) {
            log.debug("OpenCV face cropping is enabled, processing image with face detection")
            val cropRatio = getCropRatio(type)
            imageCropHelper!!.cropFaceToAspectRatio(inputImage, taskName, cropRatio)
        } else {
            log.info("OpenCV face cropping is disabled or FaceCropper not available, using original image")
            inputImage
        }

        val requestFilename = taskName + "-" + aesCipherHelper.encrypt("sudoku-${taskName}") + ".jpg"
        val requestImageUrl = s3Helper.uploadBufferedImage(
            bucket,
            "request-images/$requestFilename",
            requestImage, "jpg"
        )
        return requestImageUrl
    }

    suspend fun createTask(type: TaskType,
                           requestImageUrl: String): Task {
        val adminConfig = adminConfigHelper.getAdminConfig()
        val serviceOnline = when (type) {
            TaskType.STYLED_IMAGE -> adminConfig.imageServiceOnline
            TaskType.VIDEO_EFFECT -> adminConfig.videoServiceOnline
        }
        if (!serviceOnline) {
            throw IllegalStateException("Service is offline for type: $type")
        }

        val taskName = requestImageUrl.substringAfter("request-images/").substringBefore("-")
        log.info("Generated task name: $taskName for type: $type")

        val openApiRecords = doCreateTask(requestImageUrl)
        val filename = requestImageUrl.substringAfterLast("/")
        val task = Task(
            id = IdUtils.generateId(),
            name = taskName,
            input = TaskInput(
                type = type,
                image = requestImageUrl,
            ),
            taskIds = openApiRecords.map { it.taskId },
            openApiResultMap = openApiRecords.associateBy { it.taskId },
            status = TaskStatus.SUBMITTED,
            type = type,
            filename = filename,
            createTime = Instant.now(),
            updateTime = Instant.now(),
        )

        val result = taskRepository.setTask(task)
        log.info("Create task in taskRepository: ${task.name}, status: ${task.status}, result: $result")
        return task
    }

    suspend fun queryTask(type: TaskType, name: String, locale: Locale): Task {
        val task = taskRepository.getTask(name)
        if (task == null || task.type != type) {
            throw IllegalArgumentException("Task not found or type mismatch")
        }

        if (task.status in setOf(TaskStatus.SUCCEED, TaskStatus.FAILED)) {
            log.info("Task ${task.name} is already finished with status: ${task.status}")
            return task // No need to query if the task is already completed
        }

        val taskIds = task.taskIds

        val (overallStatus, taskQueryContext) = doQueryTask(taskIds, name)
        if (overallStatus == task.status) {
            log.debug("Task ${task.name} status has not changed, returning existing task")
            return task // No status change, return existing task
        }

        val now = Instant.now()
        val newTask = task.copy(
            status = overallStatus,
            updateTime = now,
            elapsedTimeInSeconds = Duration.between(task.createTime, now).seconds
        )
        taskQueryContext.taskResponseMap.forEach { (taskId, response) ->
            if (!response.taskResult.images.isNullOrEmpty()) {
                newTask.openApiResultMap[taskId]?.outputImage =
                    response.taskResult.images[0].url
            }
        }
        val result = taskRepository.setTask(newTask)
        log.info("Update task status in taskRepository: ${newTask.name}, status: ${newTask.status}, result: $result")
        if (overallStatus != TaskStatus.SUCCEED) {
            return newTask
        }

        log.info("Task succeed, name: ${newTask.name}, elapsedTimeInSeconds: ${newTask.elapsedTimeInSeconds}")
        newTask.openApiResultMap.forEach { (taskId, record) ->
            log.info("OpenApiResult record ${taskId}: ${ObjectMapperUtils.toJSON(record)}")
        }

        val (url, thumbnailUrl) = generateOutputUrl(name, taskQueryContext, locale)
        val finalTask = newTask.copy(
            outputs = TaskOutput(
                type = getTaskOutputType(type),
                url = url,
                thumbnailUrl = thumbnailUrl
            ),
            updateTime = Instant.now()
        )
        val result2 = taskRepository.setTask(finalTask)
        log.info("Update task outputs in taskRepository: ${newTask.name}, status: ${newTask.status}, result: $result2")

        val casting = castingRepository.addToCastingQueue(finalTask)
        log.debug("Added task ${finalTask.name} to casting queue, casting: $casting")
        return finalTask
    }

    fun printTask(type: TaskType, name: String, fromConsole: Boolean = false): Printing {
        val task = taskRepository.getTask(name)
        if (task == null || task.type != type) {
            throw IllegalArgumentException("Task not found or type mismatch")
        }

        return when (type) {
            TaskType.STYLED_IMAGE -> {
                printingRepository.addTaskToPrintingQueue(task, fromConsole)
            }
            TaskType.VIDEO_EFFECT ->
                throw UnsupportedOperationException("Printing for VIDEO_EFFECT tasks not supported")
        }
    }

    abstract suspend fun doCreateTask(requestImageUrl: String): List<OpenApiRecord>

    abstract suspend fun doQueryTask(taskIds: List<String>, taskName: String): Pair<TaskStatus, QueryTaskContext>

    abstract suspend fun generateOutputUrl(taskName: String,
                                           queryTaskContext: QueryTaskContext,
                                           locale: Locale): Pair<String, String>

    private fun getTaskOutputType(type: TaskType): TaskOutputType {
        return when (type) {
            TaskType.STYLED_IMAGE -> TaskOutputType.IMAGE
            TaskType.VIDEO_EFFECT -> TaskOutputType.VIDEO
        }
    }

    private fun getCropRatio(type: TaskType): Double {
        return when (type) {
            TaskType.STYLED_IMAGE -> 2.0 / 3.0
            TaskType.VIDEO_EFFECT -> 9.0 / 16.0
        }
    }
}