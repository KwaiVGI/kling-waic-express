package com.kling.waic.entity

data class TaskOperateInput (
    val name: String,
    val action: TaskOperateAction
)

enum class TaskOperateAction{
    PIN,
    UNPIN,
    TOP,
    DELETE
}
