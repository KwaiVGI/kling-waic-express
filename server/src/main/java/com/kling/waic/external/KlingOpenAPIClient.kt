package com.kling.waic.external

import com.kling.waic.external.model.ImageTaskRequest
import com.kling.waic.external.model.ImageTaskResponse
import com.kling.waic.external.model.Message
import com.kling.waic.repositories.JWTRepository
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
    private val styleImagePrompts: List<String>,
) {

    @Throws(IOException::class)
    fun createImageTask(imageTaskRequest: ImageTaskRequest): Message<ImageTaskResponse> {
        val url = "$baseUrl/v1/images/generations"
        val token = jwtRepository.getLatest()

        val body = ObjectMapperUtils.toJSON(
            imageTaskRequest.copy(
                prompt = styleImagePrompts.random()
            ))!!

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody(CONTENT_TYPE))
            .build()

        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException(
                    "Request failed, code: ${resp.code}, body: ${resp.body?.string()}"
                )
            }
            return resp.body
                ?.let { Message.ok<ImageTaskResponse>(it.string()) }
                ?: throw IOException("Response body is empty")
        }
    }

    companion object {
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}