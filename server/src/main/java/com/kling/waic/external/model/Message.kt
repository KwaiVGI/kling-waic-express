package com.kling.waic.external.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.kling.waic.utils.ObjectMapperUtils

data class Message<T>(
    val code: Int = 0,
    val message: String = "",
    @JsonProperty(value = "request_id")
    val requestId: String = "",
    val data: T? = null
) {
    companion object {
        fun ok(): Message<Nothing> {
            return Message(code = 200, message = "success")
        }

        inline fun <reified T> ok(data: String): Message<T> {
            return ObjectMapperUtils.fromJSON(
                data,
                object : TypeReference<Message<T>>() {})!!
        }
    }
}