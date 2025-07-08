package com.kling.waic.entities

data class Printing(
    val id: Long,
    val task: Task,
    val status: PrintingStatus
)