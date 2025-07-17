package com.kling.waic.entity

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Casting(
    val id: Long,
    val name: String,
    val task: Task,
    val score: Double,
)

data class CastingListResult(
    val total: Int,
    val score: Double?,
    val hasMore: Boolean,
    val castings: List<Casting>,
)
