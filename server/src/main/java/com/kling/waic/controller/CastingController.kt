package com.kling.waic.controller

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entity.Result
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskOperateInput
import com.kling.waic.entity.TaskType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/castings")
class CastingController {

    // image/video
    @GetMapping("{type}")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getNext(@PathVariable type: TaskType,
                @RequestParam num: Long): Result<List<Task>> {
        // TODO:
        return Result(null)
    }

    // image/video
    @GetMapping("{type}/list")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getList(@PathVariable type: TaskType,
                @RequestParam keyword: String,
                @RequestParam pageSize: Long,
                @RequestParam pageNum: Long): Result<List<Task>> {
        // TODO:
        return Result(null)
    }

    // image/video
    @PostMapping("{type}/operate")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun pin(@PathVariable type: TaskType,
            @RequestBody taskOperateInput: TaskOperateInput): Result<Boolean> {
        // TODO:
        return Result(null)
    }
}