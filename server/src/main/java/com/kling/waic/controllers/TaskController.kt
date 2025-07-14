package com.kling.waic.controllers

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entities.Printing
import com.kling.waic.entities.Result
import com.kling.waic.entities.Task
import com.kling.waic.entities.TaskType
import com.kling.waic.helper.TaskServiceSelector
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/tasks")
class TaskController (
    private val taskServiceSelector: TaskServiceSelector
) {

    @PostMapping("{type}/new")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun newTask(@PathVariable type: TaskType,
                @RequestParam("file") file: MultipartFile): Result<Task> {

        val task = taskServiceSelector.selectTaskService(type)
            .createTask(type, file)
        return Result(task)
    }

    @GetMapping("{type}/{name}")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun getTask(@PathVariable type: TaskType,
                @PathVariable name: String): Result<Task> {

        val task = taskServiceSelector.selectTaskService(type)
            .queryTask(type, name)
        return Result(task)
    }

    @PostMapping("{type}/{name}/print")
    fun printTask(
        @PathVariable type: TaskType,
        @PathVariable name: String): Result<Printing> {
        // only support printing Images
        return Result(null)
    }

//    @PostMapping("{name}/delete")
//    fun deleteTask(
//        @PathVariable name: String): Result<Boolean> {
//        return Result(null)
//    }

}