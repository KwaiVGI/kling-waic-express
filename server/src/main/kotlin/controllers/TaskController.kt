package com.klingai.express.controllers

import com.klingai.express.entities.Printing
import com.klingai.express.entities.Result
import com.klingai.express.entities.Task
import com.klingai.express.entities.TaskInput
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
    fun getTask(@PathVariable name: String): Result<Task> {
        // TODO:
        return Result(null)
    }

    @PostMapping("{name}/print")
    fun printTask(
        @PathVariable name: String): Result<Printing> {
        return Result(null)
    }
}