package com.kling.waic.service

import com.kling.waic.entity.Locale
import com.kling.waic.entity.Printing
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskType
import org.springframework.web.multipart.MultipartFile

interface TaskService {

    suspend fun createTask(type: TaskType, file: MultipartFile): Task

    suspend fun queryTask(type: TaskType, name: String, locale: Locale): Task

    suspend fun printTask(type: TaskType, name: String, fromConsole: Boolean = false): Printing
}