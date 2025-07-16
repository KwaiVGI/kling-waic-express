package com.kling.waic.external.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryVideoTaskRequest(
    @JsonProperty("task_id")
    val taskId: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryVideoTaskResponse(
    @JsonProperty("task_id")
    val taskId: String,

    @JsonProperty("task_status")
    val taskStatus: KlingOpenAPITaskStatus,

    @JsonProperty("task_status_msg")
    val taskStatusMsg: String? = null,

    @JsonProperty("task_info")
    val taskInfo: KlingTaskInfo? = null,

    @JsonProperty("task_result")
    val taskResult: QueryVideoTaskResult,

    @JsonProperty("created_at")
    val createdAt: Long,

    @JsonProperty("updated_at")
    val updatedAt: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryVideoTaskResult(
    @JsonProperty("videos")
    val videos: List<QueryVideoTaskVideo>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryVideoTaskVideo(
    @JsonProperty("id")
    val id: String = "",

    @JsonProperty("url")
    val url: String = "",

    @JsonProperty("duration")
    val duration: String = "",
)
