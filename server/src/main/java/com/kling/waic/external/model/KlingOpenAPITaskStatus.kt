package com.kling.waic.external.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

enum class KlingOpenAPITaskStatus{
    submitted,
    processing,
    succeed,
    failed
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KlingTaskInfo(
    @JsonProperty("external_task_id")
    val externalTaskId: String? = null,
)

