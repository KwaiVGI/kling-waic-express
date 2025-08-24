package com.kling.waic.component.external.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GetCurrentConcurrencyRequest (
    @JsonProperty("budget_type")
    val budgetType: BudgetType
)

enum class BudgetType {
    video,
    image
}