package com.kling.waic.component.entity

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Task (
    val id: Long,
    val name: String,
    val input: TaskInput,
    val taskIds: List<String> = emptyList(),
    val openApiResultMap: Map<String, OpenApiRecord> = emptyMap(),
    val status: TaskStatus,
    val type: TaskType,
    val filename: String,
    val outputs: TaskOutput? = null,
    val createTime: Instant,
    val updateTime: Instant,
    val elapsedTimeInSeconds: Long = 0
)

data class TaskNewInput(
    val url: String
)

data class OpenApiRecord (
    val taskId: String,
    val inputImage: String,
    val promptIndex: Int? = null,
    val prompt: String? = null,
    var outputImage: String? = null
)