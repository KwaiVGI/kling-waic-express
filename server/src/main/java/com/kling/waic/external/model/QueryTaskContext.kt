package com.kling.waic.external.model

data class QueryTaskContext (
    val taskResponseMap: Map<String, QueryImageTaskResponse> = emptyMap(),
    val video: QueryVideoTaskVideo? = null
)
