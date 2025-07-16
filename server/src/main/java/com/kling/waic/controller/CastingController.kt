package com.kling.waic.controller

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entity.Casting
import com.kling.waic.entity.CastingListResult
import com.kling.waic.entity.Result
import com.kling.waic.entity.TaskOperateInput
import com.kling.waic.entity.TaskType
import com.kling.waic.helper.CastingHelper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/castings")
class CastingController(
    private val castingHelper: CastingHelper
) {

    @GetMapping("{type}/pinned")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getPinned(@PathVariable type: TaskType): Result<Casting> {
        val casting = castingHelper.getPinned(type)
        return Result(casting)
    }

    @GetMapping("{type}/screen")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun screen(@PathVariable type: TaskType,
               @RequestParam num: Long): Result<List<Casting>> {
        val castings = castingHelper.screen(type, num)
        return Result(castings)
    }

    @GetMapping("{type}/count")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getCount(@PathVariable type: TaskType,
                @RequestParam(required = false) keyword: String = "",
                @RequestParam(required = false) score: Double? = null,
                @RequestParam pageSize: Int = 10,
                @RequestParam pageNum: Int = 1): Result<Int> {
        val result = castingHelper.count(type, keyword, score, pageSize, pageNum)
        return Result(result)
    }

    @GetMapping("{type}/list")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getList(@PathVariable type: TaskType,
                @RequestParam(required = false) keyword: String = "",
                @RequestParam(required = false) score: Double? = null,
                @RequestParam pageSize: Int = 10,
                @RequestParam pageNum: Int = 1): Result<CastingListResult> {
        val result = castingHelper.list(type, keyword, score, pageSize, pageNum)
        return Result(result)
    }

    @PostMapping("{type}/operate")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun operate(@PathVariable type: TaskType,
                @RequestBody input: TaskOperateInput): Result<Casting> {
        val casting = castingHelper.operate(type, input.name, input.action)
        return Result(casting)
    }
}