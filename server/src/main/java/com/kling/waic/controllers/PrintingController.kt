package com.kling.waic.controllers

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entities.Printing
import com.kling.waic.entities.Result
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/printings")
class PrintingController {

    @GetMapping("{name}")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getPrinting(@PathVariable name: String): Result<Printing> {
        // TODO:
        return Result(null)
    }

    @PostMapping("fetch")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun fetchPrinting(): Result<Printing> {
        // TODO:
        return Result(null)
    }

    @PostMapping("{name}/complete")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun completePrinting(): Result<Printing> {
        // TODO:
        return Result(null)
    }

}