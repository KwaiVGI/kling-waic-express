package com.kling.waic.external.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryImageTaskRequest(
    @JsonProperty("task_id")
    val taskId: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryImageTaskResponse(
    @JsonProperty("task_id")
    val taskId: String,

    @JsonProperty("task_status")
    val taskStatus: KlingOpenAPITaskStatus,

    @JsonProperty("task_status_msg")
    val taskStatusMsg: String? = null,

    @JsonProperty("task_result")
    val taskResult: QueryImageTaskResult,

    @JsonProperty("created_at")
    val createdAt: Long,

    @JsonProperty("updated_at")
    val updatedAt: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryImageTaskResult(
    @JsonProperty("images")
    val images: List<QueryImageTaskImage>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryImageTaskImage(
    @JsonProperty("index")
    val index: Int = 0,

    @JsonProperty("url")
    val url: String = ""
)
