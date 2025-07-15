package com.kling.waic.helper

import com.kling.waic.entity.TaskType
import com.kling.waic.service.TaskService
import org.springframework.stereotype.Component

@Component
class TaskServiceSelector(
    private val imageTaskService: TaskService,
    private val videoTaskService: TaskService
) {

    fun selectTaskService(type: TaskType): TaskService {
        return when (type) {
            TaskType.STYLED_IMAGE -> imageTaskService
            TaskType.VIDEO_EFFECT -> videoTaskService
        }
    }
}