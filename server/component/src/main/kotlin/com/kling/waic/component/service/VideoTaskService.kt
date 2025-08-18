package com.kling.waic.component.service

import com.kling.waic.component.entity.Locale
import com.kling.waic.component.entity.TaskStatus
import com.kling.waic.component.exception.KlingOpenAPIException
import com.kling.waic.component.external.KlingOpenAPIClient
import com.kling.waic.component.external.model.CreateVideoTaskInput
import com.kling.waic.component.external.model.CreateVideoTaskRequest
import com.kling.waic.component.external.model.KlingOpenAPITaskStatus
import com.kling.waic.component.external.model.QueryTaskContext
import com.kling.waic.component.external.model.QueryVideoTaskRequest
import com.kling.waic.component.helper.VideoResizeHelper
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.stereotype.Service

@Service
class VideoTaskService(
    private val videoSpecialEffects: List<String>,
    private val klingOpenAPIClient: KlingOpenAPIClient,
    private val videoResizeHelper: VideoResizeHelper
) : TaskService() {

    override suspend fun doCreateTask(requestImageUrl: String): List<String> {
        val effectScene = videoSpecialEffects.random()
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
        log.debug("Create Video Task with effectScene: $effectScene, taskId: ${result.data?.taskId ?: "null"}")
        return listOf(result.data!!.taskId)
    }

    override suspend fun doQueryTask(
        taskIds: List<String>,
        taskName: String
    ): Pair<TaskStatus, QueryTaskContext> {
        val taskId = taskIds.first()
        val request = QueryVideoTaskRequest(taskId)
        val result = klingOpenAPIClient.queryVideoTask(request)
        if (result.code != 0) {
            throw KlingOpenAPIException(result)
        }
        log.debug(
            "Query Video Task with result, taskId: {}, taskStatus: {}",
            taskId,
            result.data?.taskStatus ?: "null"
        )

        val taskStatus = result.data!!.taskStatus
        val convertedStatus = calculateStatus(taskStatus)
        log.info("Video Task $taskName convertedStatus: $convertedStatus")

        val video = result.data.taskResult.videos?.firstOrNull()
        return Pair(convertedStatus, QueryTaskContext(video = video))
    }

    override suspend fun generateOutputUrl(
        taskName: String,
        queryTaskContext: QueryTaskContext,
        locale: Locale
    ): Pair<String, String> {
        val url = queryTaskContext.video!!.url
        val thumbnailUrl = videoResizeHelper.resizeVideoByUrl(
            taskName = taskName,
            inputUrl = url,
            targetWidth = 225,
            targetHeight = 400
        )
        return Pair(url, thumbnailUrl)
    }

    private fun calculateStatus(taskStatus: KlingOpenAPITaskStatus): TaskStatus {
        return when (taskStatus) {
            KlingOpenAPITaskStatus.submitted -> TaskStatus.SUBMITTED
            KlingOpenAPITaskStatus.processing -> TaskStatus.PROCESSING
            KlingOpenAPITaskStatus.succeed -> TaskStatus.SUCCEED
            KlingOpenAPITaskStatus.failed -> TaskStatus.FAILED
        }
    }
}