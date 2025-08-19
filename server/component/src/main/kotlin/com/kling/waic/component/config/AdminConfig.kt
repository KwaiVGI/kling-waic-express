package com.kling.waic.component.config

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdminConfig(
    val allowPrint: Boolean = true,
    val tokenExpireInSeconds: Long = 60 * 20
)
