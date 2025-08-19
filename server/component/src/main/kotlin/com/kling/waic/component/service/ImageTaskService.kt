package com.kling.waic.component.service

import com.kling.waic.component.entity.Locale
import com.kling.waic.component.entity.TaskStatus
import com.kling.waic.component.exception.KlingOpenAPIException
import com.kling.waic.component.external.KlingOpenAPIClient
import com.kling.waic.component.external.model.*
import com.kling.waic.component.helper.ImageProcessHelper
import com.kling.waic.component.repository.TaskRepository
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

enum class ImageTaskMode(val taskN: Int) {
    ALL_GENERATED(9),
    WITH_ORIGIN(8)
}

@Service
class ImageTaskService(
    private val klingOpenAPIClient: KlingOpenAPIClient,
    private val styleImagePrompts: List<String>,
    private val imageProcessHelper: ImageProcessHelper,
    @Value("\${IMAGE_TASK_MODE:WITH_ORIGIN}")
    private val imageTaskMode: ImageTaskMode,
    private val taskRepository: TaskRepository
) : TaskService() {

    override suspend fun doCreateTask(requestImageUrl: String): List<String> {
        val taskN = imageTaskMode.taskN
        val randomPrompts = styleImagePrompts.shuffled().take(taskN)

        // Use coroutineScope instead of runBlocking
        val taskIds = coroutineScope {
            val deferredResults = randomPrompts.map { prompt ->
                async(Dispatchers.IO) {
                    val request = CreateImageTaskRequest(
                        image = requestImageUrl,
                        prompt = prompt
                    )

                    val result = klingOpenAPIClient.createImageTask(request)
                    if (result.code != 0) {
                        throw KlingOpenAPIException(result)
                    }
                    log.debug(
                        "Create Image Task with image: $requestImageUrl, " +
                                "prompt: $prompt, taskId: ${result.data?.taskId ?: "null"}"
                    )
                    result.data?.taskId
                }
            }

            // await all deferred results
            val results = deferredResults.awaitAll()
            results.filterNotNull().toMutableList()
        }

        if (taskIds.size != taskN) {
            throw IllegalStateException(
                "Failed to create the expected number of tasks: $taskN, " +
                        "only created ${taskIds.size} tasks."
            )
        }
        return taskIds
    }

    override suspend fun doQueryTask(taskIds: List<String>, taskName: String): Pair<TaskStatus, QueryTaskContext> {
        // Use coroutineScope instead of runBlocking
        val taskResponseMap = coroutineScope {
            val queryTaskRequests = taskIds.map { taskId ->
                async(Dispatchers.IO) {
                    val request = QueryImageTaskRequest(taskId = taskId)

                    val result = klingOpenAPIClient.queryImageTask(request)
                    if (result.code != 0) {
                        throw KlingOpenAPIException(result)
                    }
                    log.info(
                        "Query Image Task with result, taskId: {}, taskStatus: {}, result: {}",
                        result.data?.taskId ?: "null", result.data?.taskStatus ?: "null",
                        ObjectMapperUtils.toJSON(result)
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
        log.info("Image Task $taskName overallStatus: $overallStatus, " +
                "summaryInfo: ${summaryInfo(overallStatus, summaryMap, taskIds.size)}")
        return Pair(overallStatus, QueryTaskContext(taskResponseMap = taskResponseMap))
    }

    override suspend fun generateOutputUrl(
        taskName: String,
        queryTaskContext: QueryTaskContext,
        locale: Locale
    ): Pair<String, String> {
        log.info("Generating Sudoku image URL for task: $taskName")
        return generateSudokuImageUrl(taskName, queryTaskContext.taskResponseMap, locale)
    }

    private suspend fun generateSudokuImageUrl(
        taskName: String,
        taskResponseMap: Map<String, QueryImageTaskResponse>,
        locale: Locale
    ): Pair<String, String> {
        val imageUrls: MutableList<String> = taskResponseMap.values
            .flatMap { it.taskResult.images ?: emptyList() }
            .map { it.url.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        if (imageTaskMode == ImageTaskMode.WITH_ORIGIN) {
            // add origin image to the center of imageUrls.
            val task = taskRepository.getTask(taskName)
                ?: throw IllegalStateException("Task with name $taskName not found")
            val requestImageUrl = task.input.image

            // calculate middle index of the list
            val middleIndex = imageUrls.size / 2
            imageUrls.add(middleIndex, requestImageUrl)
        }

        val outputImageUrlPair = imageProcessHelper.downloadAndCreateSudoku(
            taskName,
            imageUrls,
            locale
        )
        return outputImageUrlPair
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
                    "${summaryMap[KlingOpenAPITaskStatus.failed]?.size ?: 0} / $totalCount, " +
                    "failed taskIds: ${summaryMap[KlingOpenAPITaskStatus.failed]?.joinToString(", ") ?: "none"}."

            TaskStatus.PROCESSING -> "Tasks are still processing, " +
                    "submitted: ${summaryMap[KlingOpenAPITaskStatus.submitted]?.size ?: 0} / $totalCount, " +
                    "processing: ${summaryMap[KlingOpenAPITaskStatus.processing]?.size ?: 0} / $totalCount, " +
                    "succeed: ${summaryMap[KlingOpenAPITaskStatus.succeed]?.size ?: 0} / $totalCount."
        }
    }
}