package com.kling.waic.component.entity

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdminConfig(
    val allowPrint: Boolean = true,
    val imageServiceOnline: Boolean = true,
    val videoServiceOnline: Boolean = true,
    val imageTokenExpireInSeconds: Long = 60 * 3,
    val videoTokenExpireInSeconds: Long = 60 * 10,
    val maxPrinterJobCount: Int = 10,
    val screenImageRatios: Pair<Int, Int> = Pair(9, 16),
    val screenVideoRatios: Pair<Int, Int> = Pair(9, 16)
)