package com.kling.waic.entity

data class Printing(
    val id: Long,
    val name: String,
    val task: Task,
    val status: PrintingStatus
)

data class PrintingUpdateInput(
    val status: PrintingStatus
)