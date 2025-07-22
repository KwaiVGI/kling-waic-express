package com.kling.waic.controller

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entity.Printing
import com.kling.waic.entity.PrintingUpdateInput
import com.kling.waic.entity.Result
import com.kling.waic.repository.PrintingRepository
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/printings")
class PrintingController(
    private val printingRepository: PrintingRepository
) {

    @PostMapping("fetch")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun fetchPrinting(): Result<Printing> {
        log.debug("Fetching printing from queue")

        val printing = printingRepository.pollOneFromPrintingQueue()
        log.debug("Fetched printing: ${printing}")

        return Result(printing)
    }

    @PostMapping("{name}/update")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun updatePrinting(@PathVariable name: String,
                       @RequestBody input: PrintingUpdateInput): Result<Printing> {
        log.debug("Update printing: ${name}, status: ${input.status}")

        val printing = printingRepository.updatePrintingStatus(name, input.status)
        log.debug("Updated printing: $printing")

        return Result(printing)
    }

    @GetMapping("{name}")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getPrinting(@PathVariable name: String): Result<Printing> {
        val printing = printingRepository.getPrinting(name)
        return Result(printing)
    }

    @GetMapping("queryAll")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun queryAll(@RequestParam(required = false) keyword: String = "",): Result<List<Printing>> {
        val allPrintings = printingRepository.queryAll(keyword)
        return Result(allPrintings)
    }
}