package com.kling.waic.component.service

import com.kling.waic.component.entity.Locale
import com.kling.waic.component.entity.TaskStatus
import com.kling.waic.component.exception.KlingOpenAPIException
import com.kling.waic.component.external.KlingOpenAPIClient
import com.kling.waic.component.external.model.*
import com.kling.waic.component.helper.ImageProcessHelper
import com.kling.waic.component.utils.Slf4j.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

@Service
class ImageTaskService(
    private val klingOpenAPIClient: KlingOpenAPIClient,
    private val styleImagePrompts: List<String>,
    private val imageProcessHelper: ImageProcessHelper,
) : TaskService() {

    companion object {
        const val TASK_N: Int = 9
    }

    override suspend fun doCreateTask(requestImageUrl: String): List<String> {
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
                    if (result.code != 0) {
                        throw KlingOpenAPIException(result)
                    }
                    log.debug(
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

        if (taskIds.size != TASK_N) {
            throw IllegalStateException(
                "Failed to create the expected number of tasks: $TASK_N, " +
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
        log.info("Task $taskName overallStatus: $overallStatus, " +
                "summaryInfo: ${summaryInfo(overallStatus, summaryMap, taskIds.size)}")
        return Pair(overallStatus, QueryTaskContext(taskResponseMap = taskResponseMap))
    }

    override suspend fun generateOutputUrl(
        taskName: String,
        queryTaskContext: QueryTaskContext,
        locale: Locale
    ): String {
        log.info("Generating Sudoku image URL for task: $taskName")
        return generateSudokuImageUrl(taskName, queryTaskContext.taskResponseMap, locale)
    }

    private suspend fun generateSudokuImageUrl(
        taskName: String,
        taskResponseMap: Map<String, QueryImageTaskResponse>,
        locale: Locale
    ): String {
        val imageUrls: List<String> = taskResponseMap.values
            .flatMap { it.taskResult.images ?: emptyList() }
            .map { it.url }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val outputImageUrl = imageProcessHelper.downloadAndCreateSudoku(
            taskName,
            imageUrls,
            locale
        )
        return outputImageUrl
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