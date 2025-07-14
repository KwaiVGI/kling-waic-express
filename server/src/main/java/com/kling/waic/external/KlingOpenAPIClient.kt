package com.kling.waic.external

import com.kling.waic.external.model.CreateImageTaskRequest
import com.kling.waic.external.model.CreateImageTaskResponse
import com.kling.waic.external.model.KlingOpenAPIResult
import com.kling.waic.external.model.QueryImageTaskRequest
import com.kling.waic.external.model.QueryImageTaskResponse
import com.kling.waic.repository.JWTRepository
import com.kling.waic.utils.ObjectMapperUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class KlingOpenAPIClient(
    @Value("\${kling.open-api.base-url}") private val baseUrl: String,
    private val jwtRepository: JWTRepository,
    private val okHttpClient: OkHttpClient,
) {

    companion object {
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    @Throws(IOException::class)
    fun createImageTask(createImageTaskRequest: CreateImageTaskRequest):
            KlingOpenAPIResult<CreateImageTaskResponse> {
        val url = "$baseUrl/v1/images/generations"

        val body = ObjectMapperUtils.toJSON(createImageTaskRequest)!!
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${jwtRepository.getLatest()}")
            .post(body.toRequestBody(CONTENT_TYPE))
            .build()

        okHttpClient.newCall(request).execute().use { resp ->
            return resp.body
                ?.let { KlingOpenAPIResult.ok<CreateImageTaskResponse>(it.string()) }
                ?: throw IOException("Response body is empty")
        }
    }

    @Throws(IOException::class)
    fun queryImageTask(queryImageTaskRequest: QueryImageTaskRequest):
            KlingOpenAPIResult<QueryImageTaskResponse> {
        val url = "$baseUrl/v1/images/generations/${queryImageTaskRequest.taskId}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${jwtRepository.getLatest()}")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { resp ->
            return resp.body
                ?.let { KlingOpenAPIResult.ok<QueryImageTaskResponse>(it.string()) }
                ?: throw IOException("Response body is empty")
        }
    }
}