package com.kling.waic.entity

import java.time.Instant

data class Result<T> (
    val data: T? = null,
    val status: ResultStatus = ResultStatus.SUCCEED,
    val message: String = "",
    val timestamp: Instant = Instant.now()
)