package com.kling.waic.entities

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
