package com.klingai.express.entities

/**
 * @author Kanle Shi <shikanle@kuaishou.com>
 * Created on 2025-07-01
 */
data class TaskInput (
    val type: String,
    val parameters: HashMap<String, Any>
)