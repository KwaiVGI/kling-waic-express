package com.klingai.express.controllers

import com.klingai.express.entities.Printing
import com.klingai.express.entities.Result
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

    @PostMapping("dequeue")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun dequeuePrinting(): Result<Printing> {
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