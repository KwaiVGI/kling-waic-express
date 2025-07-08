package com.kling.waic.entities

data class TaskInput (
    val type: TaskType,
    val parameters: HashMap<String, Any>
)