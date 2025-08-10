package com.kling.waic.entity

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Printing(
    val id: Long,
    val name: String,
    val task: Task,
    val status: PrintingStatus,
    val aheadCount: Int? = null,
)

data class PrintingUpdateInput(
    val status: PrintingStatus
)

data class SetPrinterQueuedJobCountRequest(
    val printerQueuedJobCount: Int
)