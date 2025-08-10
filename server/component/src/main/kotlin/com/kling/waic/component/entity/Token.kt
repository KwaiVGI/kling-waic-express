package com.kling.waic.component.entity

import java.time.Instant

data class Token (
    val id: Long,
    val value: String,
    val createTime: Instant,
    val refreshTime: Instant,
    val expireTime: Instant
)