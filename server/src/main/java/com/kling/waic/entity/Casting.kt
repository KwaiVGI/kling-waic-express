package com.kling.waic.entity

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Casting(
    val id: Long,
    val name: String,
    val task: Task,
    val score: Double,
    val originScore: Long? = null,
)

data class CastingListResult(
    val hasMore: Boolean,
    val castings: List<Casting>,
)
