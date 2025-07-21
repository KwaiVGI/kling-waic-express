package com.kling.waic.entity

import com.kling.waic.external.model.KlingOpenAPIResult
import java.time.Instant

data class Result<T> (
    val data: T? = null,
    val status: ResultStatus = ResultStatus.SUCCEED,
    val klingOpenAPIResult: KlingOpenAPIResult<*>? = null,
    val message: String = "",
    val timestamp: Instant = Instant.now()
)