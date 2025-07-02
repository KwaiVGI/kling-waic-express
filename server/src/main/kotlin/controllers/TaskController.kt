package com.klingai.express.controllers

import com.klingai.express.entities.Result
import com.klingai.express.entities.Task
import com.klingai.express.entities.TaskInput
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/tasks")
class TaskController {

    @PostMapping("new")
    fun newTask(
        @RequestBody input: TaskInput,
        @RequestParam("file") file: MultipartFile): Result {
        return Result(null)
    }
}