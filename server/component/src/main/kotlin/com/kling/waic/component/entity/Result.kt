package com.kling.waic.component.entity

import com.fasterxml.jackson.core.type.TypeReference
import com.kling.waic.component.external.model.KlingOpenAPIResult
import com.kling.waic.component.utils.ObjectMapperUtils
import java.time.Instant

data class Result<T> (
    val data: T? = null,
    val status: ResultStatus = ResultStatus.SUCCEED,
    val klingOpenAPIResult: KlingOpenAPIResult<*>? = null,
    val message: String = "",
    val timestamp: Instant = Instant.now()
) {

    companion object {
        inline fun <reified T> fromJSON(data: String): Result<T> {
            return ObjectMapperUtils.fromJSON(
                data,
                object : TypeReference<Result<T>>() {})!!
        }
    }
}