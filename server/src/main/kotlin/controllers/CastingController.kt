package com.klingai.express.controllers

import com.klingai.express.auth.Authorization
import com.klingai.express.auth.AuthorizationType
import com.klingai.express.entities.Result
import com.klingai.express.entities.Task
import com.klingai.express.entities.TaskOutput
import com.klingai.express.entities.TaskType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/castings")
class CastingController {

    @GetMapping("{type}/{num}")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getNext(@PathVariable type: TaskType, @PathVariable num: Long): Result<List<TaskOutput>> {
        // TODO:
        return Result(null)
    }

    @GetMapping("{type}/list/{page}")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getList(@PathVariable type: TaskType, @PathVariable page: Long): Result<List<Task>> {
        // TODO:
        return Result(null)
    }

    @PostMapping("{type}/promote")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun promoteList(@PathVariable type: TaskType, @RequestBody taskName: String): Result<Boolean> {
        // TODO:
        return Result(null)
    }

    @PostMapping("{type}/pin")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun pin(@PathVariable type: TaskType): Result<Boolean> {
        // TODO:
        return Result(null)
    }

    @PostMapping("{type}/unpin")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun unpin(@PathVariable type: TaskType): Result<Boolean> {
        // TODO:
        return Result(null)
    }

}