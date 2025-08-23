package com.kling.waic.printer.client

import com.kling.waic.component.entity.*
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class PrintingDataClient(
    private val okHttpClient: OkHttpClient,
    @Value("\${SERVER_BASE_URI:https://waic-api.klingai.com}")
    private val serverBaseURI: String,
    @Value("\${WAIC_MANAGEMENT_ACTIVITY:}")
    private val waicManagementActivity: String,
    @Value("\${WAIC_MANAGEMENT_TOKEN}")
    private val waicManagementToken: String
) {
    companion object {
        private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    fun fetchPrinting(count: Int): List<Printing> {
        val url = "$serverBaseURI/api/printings/batch_fetch"

        val body = ObjectMapperUtils.toJSON(
            BatchFetchPrintingRequest(
                count = count
            )
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $waicManagementToken")
            .addHeader("Activity", waicManagementActivity)
            .post(body!!.toRequestBody(CONTENT_TYPE))
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("Unexpected HTTP code ${resp.code}")
                }

                val responseBody = resp.body?.string()
                    ?: throw IOException("Response body is empty")

                log.debug("fetch printing response: $responseBody")

                val response = Result.fromJSON<BatchFetchPrintingResponse>(responseBody)
                val printings = response.data!!.printings

                printings
            }
        } catch (e: Exception) {
            log.error("Error fetching printing - URL: {}", url, e)
            throw IllegalArgumentException("Error fetching printing - URL: {}", e)
        }
    }

    fun updatePrintingStatus(taskName: String, status: PrintingStatus): Printing {
        val printingName = "printing:$taskName"
        val url = "$serverBaseURI/api/printings/${printingName}/update"

        val body = ObjectMapperUtils.toJSON(
            PrintingUpdateInput(
                status = status
            )
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $waicManagementToken")
            .post(body!!.toRequestBody(CONTENT_TYPE))
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("Unexpected HTTP code ${resp.code}")
                }

                val responseBody = resp.body?.string()
                    ?: throw IOException("Response body is empty")

                log.info("update printing status response: $responseBody")

                val response = Result.fromJSON<Printing?>(responseBody)
                val printing = response.data

                printing!!
            }
        } catch (e: Exception) {
            log.error("Error update printing status - URL: {}", url, e)
            throw IllegalArgumentException("Error update printing status - URL: {}", e)
        }
    }

    fun setPrinterQueuedJobCount(
        printerQueuedJobCount: Int
    ): String {
        val url = "$serverBaseURI/api/printings/setPrinterQueuedJobCount"

        val body = ObjectMapperUtils.toJSON(
            SetPrinterQueuedJobCountRequest(
                printerQueuedJobCount = printerQueuedJobCount
            )
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $waicManagementToken")
            .post(body!!.toRequestBody(CONTENT_TYPE))
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("Unexpected HTTP code ${resp.code}")
                }

                val responseBody = resp.body?.string()
                    ?: throw IOException("Response body is empty")

                log.debug("set printer queuedJobCount: $responseBody")

                val response = Result.fromJSON<String>(responseBody)
                val result = response.data

                result!!
            }
        } catch (e: Exception) {
            log.error("Error set printer queuedJobCount - URL: {}", url, e)
            throw IllegalArgumentException("Error set printer queuedJobCount - URL: {}", e)
        }
    }
}