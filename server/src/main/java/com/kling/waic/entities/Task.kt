package com.kling.waic.entities

import java.time.Instant

data class Task (
    val id: Long,
    val name: String,
    val type: TaskType,
    val createTime: Instant,
    val status: TaskStatus,
    val input: TaskInput,
    val outputs: TaskOutput?
)