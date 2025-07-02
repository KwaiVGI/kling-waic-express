package com.klingai.express.entities

import java.time.Instant

data class Result (
    val data: Any?,
    val code: Int = 200,
    val timestamp: Instant = Instant.now()
)