package com.kling.waic.component.entity

data class TaskOperateInput (
    val name: String,
    val action: TaskOperateAction
)

enum class TaskOperateAction{
    PIN,
    UNPIN,
    PROMOTE,
    DELETE
}
