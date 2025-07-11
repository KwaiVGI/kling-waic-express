package com.kling.waic.controllers

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entities.Printing
import com.kling.waic.entities.Result
import com.kling.waic.entities.Task
import com.kling.waic.entities.TaskInput
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/tasks")
class TaskController {

    @PostMapping("new")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun newTask(
        @RequestBody input: TaskInput,
        @RequestParam("file") file: MultipartFile): Result<Task> {
        return Result(null)
    }

    @GetMapping("{name}")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun getTask(@PathVariable name: String): Result<Task> {
        // TODO:
        return Result(null)
    }

    @PostMapping("{name}/print")
    fun printTask(
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