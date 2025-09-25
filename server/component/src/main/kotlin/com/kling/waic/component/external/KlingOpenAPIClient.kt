package com.kling.waic.component.external

import com.kling.waic.component.external.model.CreateImageTaskRequest
import com.kling.waic.component.external.model.CreateImageTaskResponse
import com.kling.waic.component.external.model.CreateVideoTaskRequest
import com.kling.waic.component.external.model.CreateVideoTaskResponse
import com.kling.waic.component.external.model.GetCurrentConcurrencyRequest
import com.kling.waic.component.external.model.KlingOpenAPIResult
import com.kling.waic.component.external.model.QueryImageTaskRequest
import com.kling.waic.component.external.model.QueryImageTaskResponse
import com.kling.waic.component.external.model.QueryVideoTaskRequest
import com.kling.waic.component.external.model.QueryVideoTaskResponse
import com.kling.waic.component.helper.JWTHelper
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class KlingOpenAPIClient(
    @param:Value("\${KLING_OPEN_BASE_URL:https://api-beijing.klingai.com}") private val baseUrl: String,
    private val jwtHelper: JWTHelper,
    private val okHttpClient: OkHttpClient,
) {

    companion object {
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    @Throws(IOException::class)
    fun getCurrentConcurrency(getCurrentConcurrencyRequest: GetCurrentConcurrencyRequest):
            KlingOpenAPIResult<Long> {
        val url = "$baseUrl/account/concurrency".toHttpUrl().newBuilder()
            .addQueryParameter("budget_type", getCurrentConcurrencyRequest.budgetType.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${jwtHelper.getLatest()}")
            .get()
            .build()

        try {
            okHttpClient.newCall(request).execute().use { resp ->
                val responseBody = resp.body?.string()
                log.debug("getCurrentConcurrency responseBody: {}", responseBody)

                return responseBody
                    ?.let { KlingOpenAPIResult.ok<Long>(it) }
                    ?: throw IOException("Response body is empty")
            }
        } catch (e: java.net.SocketTimeoutException) {
            log.error("Socket timeout when querying image task - URL: {}", url, e)
            throw IOException("Request timeout when querying image task", e)
        } catch (e: Exception) {
            log.error("Error querying image task - URL: {}", url, e)
            throw e
        }
    }

    @Throws(IOException::class)
    suspend fun createImageTask(createImageTaskRequest: CreateImageTaskRequest):
            KlingOpenAPIResult<CreateImageTaskResponse> {
        // Remove withContext to preserve original thread
        val url = "$baseUrl/v1/images/generations"

        val body = ObjectMapperUtils.toJSON(createImageTaskRequest)!!
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${jwtHelper.getLatest()}")
            .post(body.toRequestBody(CONTENT_TYPE))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { resp ->
                val responseBody = resp.body?.string()
                if (createImageTaskRequest.image.startsWith("http")) {
                    log.debug(
                        "Create OpenAPI image task with image url: ${createImageTaskRequest.image}, " +
                                "prompt: ${createImageTaskRequest.prompt}, responseBody: $responseBody"
                    )
                } else {
                    log.debug(
                        "Create OpenAPI image task with base64 image data size: " +
                                "${createImageTaskRequest.image.length}, " +
                                "prompt: ${createImageTaskRequest.prompt}, responseBody: $responseBody"
                    )
                }

                return responseBody
                    ?.let {
                        KlingOpenAPIResult.ok<CreateImageTaskResponse>(it)
                    }
                    ?: throw IOException("Response body is empty")
            }
        } catch (e: java.net.SocketTimeoutException) {
            log.error("Socket timeout when creating image task - URL: {}", url, e)
            throw IOException("Request timeout when creating image task", e)
        } catch (e: Exception) {
            log.error("Error creating image task - URL: {}", url, e)
            throw e
        }
    }

    @Throws(IOException::class)
    suspend fun queryImageTask(queryImageTaskRequest: QueryImageTaskRequest):
            KlingOpenAPIResult<QueryImageTaskResponse> {
        // Remove withContext to preserve original thread
        val url = "$baseUrl/v1/images/generations/${queryImageTaskRequest.taskId}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${jwtHelper.getLatest()}")
            .get()
            .build()

        try {
            okHttpClient.newCall(request).execute().use { resp ->
                val responseBody = resp.body?.string()
                log.debug("queryImageTask responseBody: {}", responseBody)

                return responseBody
                    ?.let { KlingOpenAPIResult.ok<QueryImageTaskResponse>(it) }
                    ?: throw IOException("Response body is empty")
            }
        } catch (e: java.net.SocketTimeoutException) {
            log.error("Socket timeout when querying image task - URL: {}", url, e)
            throw IOException("Request timeout when querying image task", e)
        } catch (e: Exception) {
            log.error("Error querying image task - URL: {}", url, e)
            throw e
        }
    }

    @Throws(IOException::class)
    suspend fun createVideoTask(createVideoTaskRequest: CreateVideoTaskRequest):
            KlingOpenAPIResult<CreateVideoTaskResponse> {
        // Remove withContext to preserve original thread
        val url = "$baseUrl/v1/videos/effects"

        val body = ObjectMapperUtils.toJSON(createVideoTaskRequest)!!
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${jwtHelper.getLatest()}")
            .post(body.toRequestBody(CONTENT_TYPE))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { resp ->
                val responseBody = resp.body?.string()
                if (createVideoTaskRequest.input.image.startsWith("http")) {
                    log.debug(
                        "Create OpenAPI video task with image url: ${createVideoTaskRequest.input.image}, " +
                                "responseBody: $responseBody"
                    )
                } else {
                    log.debug(
                        "Create OpenAPI video task with base64 image data size: " +
                                "${createVideoTaskRequest.input.image.length}, " +
                                "responseBody: $responseBody"
                    )
                }

                return responseBody
                    ?.let { KlingOpenAPIResult.ok<CreateVideoTaskResponse>(it) }
                    ?: throw IOException("Response body is empty")
            }
        } catch (e: java.net.SocketTimeoutException) {
            log.error("Socket timeout when creating video task - URL: {}", url, e)
            throw IOException("Request timeout when creating video task", e)
        } catch (e: Exception) {
            log.error("Error creating video task - URL: {}", url, e)
            throw e
        }
    }

    @Throws(IOException::class)
    suspend fun queryVideoTask(queryVideoTaskRequest: QueryVideoTaskRequest):
            KlingOpenAPIResult<QueryVideoTaskResponse> {
        // Remove withContext to preserve original thread
        val url = "$baseUrl/v1/videos/effects/${queryVideoTaskRequest.taskId}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${jwtHelper.getLatest()}")
            .get()
            .build()

        try {
            okHttpClient.newCall(request).execute().use { resp ->
                val responseBody = resp.body?.string()
                log.debug("queryVideoTask responseBody: {}", responseBody)

                return responseBody
                    ?.let { KlingOpenAPIResult.ok<QueryVideoTaskResponse>(it) }
                    ?: throw IOException("Response body is empty")
            }
        } catch (e: java.net.SocketTimeoutException) {
            log.error("Socket timeout when querying video task - URL: {}", url, e)
            throw IOException("Request timeout when querying video task", e)
        } catch (e: Exception) {
            log.error("Error querying video task - URL: {}", url, e)
            throw e
        }
    }

}