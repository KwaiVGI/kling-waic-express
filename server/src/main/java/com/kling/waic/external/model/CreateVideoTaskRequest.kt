package com.kling.waic.external.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateVideoTaskRequest(
    @JsonProperty("effect_scene")
    val effectScene: String,

    @JsonProperty("input")
    val input: CreateVideoTaskInput,

    @JsonProperty("callback_url")
    val callBackUrl: String? = null,

    @JsonProperty("external_task_id")
    val externalTaskId: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateVideoTaskInput(
    @JsonProperty("model_name")
    val modelName: String = "kling-v1-6",

    @JsonProperty("image")
    val image: String,

    @JsonProperty("duration")
    val duration: String = "5", //5, 10
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateVideoTaskResponse(
    @JsonProperty("task_id")
    val taskId: String = "",

    @JsonProperty("task_status")
    val taskStatus: KlingOpenAPITaskStatus,

    @JsonProperty("task_info")
    val taskInfo: KlingTaskInfo? = null,

    @JsonProperty("created_at")
    val createdAt: Long = 0,

    @JsonProperty("updated_at")
    val updatedAt: Long = 0,
)