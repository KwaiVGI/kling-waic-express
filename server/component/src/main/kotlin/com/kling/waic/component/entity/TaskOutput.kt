package com.kling.waic.component.entity

data class TaskInput (
    val type: TaskType,
    val image: String,
)

data class TaskOutput (
    val type: TaskOutputType,
    val url: String,
    val thumbnailUrl: String? = null
)
