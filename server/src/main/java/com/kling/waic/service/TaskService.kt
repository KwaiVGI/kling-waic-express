package com.kling.waic.service

import com.kling.waic.entities.Task
import com.kling.waic.entities.TaskType
import org.springframework.web.multipart.MultipartFile

interface TaskService {

    fun createTask(type: TaskType, file: MultipartFile): Task

    fun queryTask(type: TaskType, name: String): Task
}