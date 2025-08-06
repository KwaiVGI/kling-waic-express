package com.kling.waic.service

import com.google.errorprone.annotations.concurrent.LazyInit
import com.kling.waic.entity.Locale
import com.kling.waic.entity.Printing
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskInput
import com.kling.waic.entity.TaskOutput
import com.kling.waic.entity.TaskOutputType
import com.kling.waic.entity.TaskStatus
import com.kling.waic.entity.TaskType
import com.kling.waic.external.model.QueryTaskContext
import com.kling.waic.helper.AESCipherHelper
import com.kling.waic.helper.ImageCropHelper
import com.kling.waic.helper.ImageProcessHelper
import com.kling.waic.helper.S3Helper
import com.kling.waic.repository.CastingRepository
import com.kling.waic.repository.CodeGenerateRepository
import com.kling.waic.repository.PrintingRepository
import com.kling.waic.repository.TaskRepository
import com.kling.waic.utils.IdUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
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
    @Value("\${s3.bucket}")
    private lateinit var bucket: String
    @Value("\${waic.crop-image-with-opencv}")
    private lateinit var cropImageWithOpenCV: String
    @Autowired
    private lateinit var aesCipherHelper: AESCipherHelper

    @Autowired(required = false)
    @LazyInit
    private var imageCropHelper: ImageCropHelper? = null

    fun uploadImage(type: TaskType, file: MultipartFile): String {
        val taskName = codeGenerateRepository.nextCode(type)
        val inputImage = imageProcessHelper.multipartFileToBufferedImage(file)

        val requestImage = if (cropImageWithOpenCV.toBoolean() && imageCropHelper != null) {
            log.info("OpenCV face cropping is enabled, processing image with face detection")
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
        val taskName = requestImageUrl.substringAfterLast("/")
            .removeSuffix(".jpg")
            .removePrefix("request-")
        log.info("Generated task name: $taskName for type: $type")

        val taskIds = doCreateTask(requestImageUrl)
        val filename = requestImageUrl.substringAfterLast("/")
        val task = Task(
            id = IdUtils.generateId(),
            name = taskName,
            input = TaskInput(
                type = type,
                image = requestImageUrl,
            ),
            taskIds = taskIds,
            status = TaskStatus.SUBMITTED,
            type = type,
            filename = filename,
            createTime = Instant.now(),
            updateTime = Instant.now(),
        )

        taskRepository.setTask(task)
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

        val newTask = task.copy(
            status = overallStatus,
            updateTime = Instant.now(),
        )
        taskRepository.setTask(newTask)
        if (overallStatus != TaskStatus.SUCCEED) {
            return newTask
        }

        val url = generateOutputUrl(name, taskQueryContext, locale)
        val finalTask = newTask.copy(
            outputs = TaskOutput(
                type = getTaskOutputType(type),
                url = url
            ),
            updateTime = Instant.now()
        )
        taskRepository.setTask(finalTask)

        val casting = castingRepository.addToCastingQueue(finalTask)
        log.info("Added task ${finalTask.name} to casting queue, casting: $casting")
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

    abstract suspend fun doCreateTask(requestImageUrl: String): List<String>

    abstract suspend fun doQueryTask(taskIds: List<String>, taskName: String): Pair<TaskStatus, QueryTaskContext>

    abstract suspend fun generateOutputUrl(taskName: String, queryTaskContext: QueryTaskContext, locale: Locale): String

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