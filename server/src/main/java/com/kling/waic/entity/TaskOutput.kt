package com.kling.waic.entity

data class TaskInput (
    val type: TaskType,
    val image: String,
)

data class TaskOutput (
    val type: TaskOutputType,
    val url: String
)
