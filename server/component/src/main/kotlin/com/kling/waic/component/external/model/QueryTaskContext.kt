package com.kling.waic.component.external.model

data class QueryTaskContext (
    val taskResponseMap: Map<String, QueryImageTaskResponse> = emptyMap(),
    val video: QueryVideoTaskVideo? = null
)
