package com.klingai.express.entities

data class Printing(
    val id: Long,
    val task: Task,
    val status: PrintingStatus
)