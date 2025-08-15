package com.kling.waic.component.external.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// Doc: https://docs.qingque.cn/d/home/eZQAyImcbaS0fz-8ANjXvU5ed?identityId=2ETa3ZI9xlj#section=h.kc4crsy4j7to
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateImageTaskRequest(
    @JsonProperty("model_name")
    val modelName: String = "kling-v2-new",

    @JsonProperty("prompt")
    val prompt: String,

    @JsonProperty("negative_prompt")
    val negativePrompt: String? = null,

    @JsonProperty("image")
    val image: String,

    @JsonProperty("image_reference")
    val imageReference: String? = null,

    @JsonProperty("image_fidelity")
    val imageFidelity: Float? = null,

    @JsonProperty("human_fidelity")
    val humanFidelity: Float? = null,

    @JsonProperty("resolution")
    val resolution: String = "1k", // 1k, 2k

    @JsonProperty("n")
    val n: Int = 1,

    @JsonProperty("aspect_ratio")
    val aspectRatio: String = "2:3", // 1:1, 16:9, etc.

    @JsonProperty("call_back_url")
    val callBackUrl: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateImageTaskResponse(
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
