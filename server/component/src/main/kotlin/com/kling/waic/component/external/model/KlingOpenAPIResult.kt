package com.kling.waic.component.external.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.kling.waic.component.utils.ObjectMapperUtils

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KlingOpenAPIResult<T>(
    val code: Int = 0,
    val message: String = "",
    @JsonProperty(value = "request_id")
    val requestId: String? = "",
    val data: T? = null
) {
    companion object {
        fun ok(): KlingOpenAPIResult<Nothing> {
            return KlingOpenAPIResult(code = 200, message = "success")
        }

        inline fun <reified T> ok(data: String): KlingOpenAPIResult<T> {
            return ObjectMapperUtils.fromJSON(
                data,
                object : TypeReference<KlingOpenAPIResult<T>>() {})!!
        }
    }
}