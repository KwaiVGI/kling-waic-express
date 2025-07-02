package com.klingai.express.entities

import java.time.Instant

data class Result<T> (
    val data: T?,
    val status: ResultStatus = ResultStatus.SUCCEED,
    val timestamp: Instant = Instant.now()
)