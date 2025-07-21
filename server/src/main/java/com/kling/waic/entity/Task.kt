package com.kling.waic.entity

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Task (
    val id: Long,
    val name: String,
    val input: TaskInput,
    val taskIds: List<String>,
    val status: TaskStatus,
    val type: TaskType,
    val filename: String,
    val outputs: TaskOutput? = null,
    val createTime: Instant,
    val updateTime: Instant,
)