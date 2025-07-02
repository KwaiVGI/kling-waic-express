package com.klingai.express.entities

data class TaskInput (
    val type: TaskType,
    val parameters: HashMap<String, Any>
)