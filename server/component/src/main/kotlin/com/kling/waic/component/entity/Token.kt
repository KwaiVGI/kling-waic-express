package com.kling.waic.component.entity

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Token (
    val id: Long,
    val value: String,
    val createTime: Instant,
    val refreshTime: Instant,
    val expireTime: Instant,
    val activity: String? = null
)