package com.kling.waic.controller

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entity.Printing
import com.kling.waic.entity.PrintingUpdateInput
import com.kling.waic.entity.Result
import com.kling.waic.helper.PrintingHelper
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/printings")
class PrintingController(
    private val printingHelper: PrintingHelper
) {

    @PostMapping("fetch")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun fetchPrinting(): Result<Printing> {
        log.info("Fetching printing from queue")

        val printing = printingHelper.pollOneFromPrintingQueue()
        log.info("Fetched printing: ${printing}")

        return Result(printing)
    }

    @PostMapping("{name}/update")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun updatePrinting(@PathVariable name: String,
                       @RequestBody input: PrintingUpdateInput): Result<Printing> {
        log.info("Update printing: ${name}, status: ${input.status}")

        val printing = printingHelper.updatePrintingStatus(name, input.status)
        log.info("Updated printing: $printing")

        return Result(printing)
    }

//    @GetMapping("{name}")
//    @Authorization(AuthorizationType.MANAGEMENT)
//    fun getPrinting(@PathVariable name: String): Result<Printing> {
//        // TODO:
//        return Result(null)
//    }
}