package com.klingai.express.entities

import java.time.Instant

data class Task (
    val id: Long,
    val name: String,
    val createTime: Instant,
    val input: TaskInput,
    val outputs: List<TaskOutput>
)