package com.klingai.express.entities

import java.time.Instant

data class Token (
    val id: Long,
    val name: String,
    val createTime: Instant,
    val refreshTime: Instant,
    val expireTime: Instant
)