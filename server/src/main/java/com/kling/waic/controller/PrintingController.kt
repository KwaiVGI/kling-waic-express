package com.kling.waic.controller

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entity.Printing
import com.kling.waic.entity.PrintingUpdateInput
import com.kling.waic.entity.Result
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/printings")
class PrintingController {

    @PostMapping("fetch")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun fetchPrinting(): Result<Printing> {
        // TODO:
        return Result(null)
    }

    @PostMapping("{name}/update")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun completePrinting(
        @RequestBody requestBody: PrintingUpdateInput): Result<Printing> {
        // TODO:
        return Result(null)
    }

//    @GetMapping("{name}")
//    @Authorization(AuthorizationType.MANAGEMENT)
//    fun getPrinting(@PathVariable name: String): Result<Printing> {
//        // TODO:
//        return Result(null)
//    }
}