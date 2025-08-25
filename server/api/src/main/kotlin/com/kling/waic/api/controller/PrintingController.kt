package com.kling.waic.api.controller

import com.kling.waic.component.auth.Authorization
import com.kling.waic.component.auth.AuthorizationType
import com.kling.waic.component.entity.BatchFetchPrintingRequest
import com.kling.waic.component.entity.BatchFetchPrintingResponse
import com.kling.waic.component.entity.Printing
import com.kling.waic.component.entity.PrintingUpdateInput
import com.kling.waic.component.entity.Result
import com.kling.waic.component.entity.SetPrinterQueuedJobCountRequest
import com.kling.waic.component.repository.PrintingRepository
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/printings")
class PrintingController(
    private val printingRepository: PrintingRepository,
) {

    @PostMapping("fetch")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun fetchPrinting(): Result<Printing> {
        log.debug("Fetching printing from queue")

        val printing = printingRepository.pollOneFromPrintingQueue()
        log.debug("Fetched printing: ${printing}")

        return Result(printing)
    }

    @PostMapping("batch_fetch")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun batchFetchPrinting(
        @RequestBody input: BatchFetchPrintingRequest
    ): Result<BatchFetchPrintingResponse> {
        log.debug("Batch fetching printing from queue")

        val printings = printingRepository.pollBatchFromPrintingQueue(input.count)
        log.debug("Batch fetched printing: ${printings}")

        return Result(BatchFetchPrintingResponse(printings))
    }

    @PostMapping("{name}/update")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun updatePrinting(
        @PathVariable name: String,
        @RequestBody input: PrintingUpdateInput
    ): Result<Printing> {
        log.debug("Update printing: ${name}, status: ${input.status}")

        val printing = printingRepository.updatePrintingStatus(name, input.status)
        log.debug("Updated printing: $printing")

        return Result(printing)
    }

    @GetMapping("{name}")
    fun getPrinting(@PathVariable name: String): Result<Printing> {
        val printing = printingRepository.getPrinting(name)
        return Result(printing)
    }

    @GetMapping("queryAll")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun queryAll(@RequestParam(required = false) keyword: String = ""): Result<List<Printing>> {
        val allPrintings = printingRepository.queryAll(keyword)
        return Result(allPrintings)
    }

    @GetMapping("getPrinterQueuedJobCount")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun getPrinterQueuedJobCount(): Result<Int> {
        val printerQueuedJobCount = printingRepository.getPrinterQueuedJobCount()
        return Result(printerQueuedJobCount)
    }

    @PostMapping("setPrinterQueuedJobCount")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun setPrinterQueuedJobCount(
        @RequestBody request: SetPrinterQueuedJobCountRequest
    ): Result<String> {
        val printerQueuedJobCount = printingRepository.setPrinterQueuedJobCount(request)
        return Result(printerQueuedJobCount)
    }
}