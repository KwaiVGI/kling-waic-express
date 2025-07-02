package com.klingai.express.controllers

import com.klingai.express.entities.Printing
import com.klingai.express.entities.Result
import com.klingai.express.entities.TaskOutput
import com.klingai.express.entities.TaskType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
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

}