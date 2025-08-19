package com.kling.waic.component.repository

import com.kling.waic.component.entity.*
import com.kling.waic.component.exception.DuplicatePrintException
import com.kling.waic.component.helper.AdminConfigHelper
import com.kling.waic.component.utils.IdUtils
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.stereotype.Repository
import redis.clients.jedis.commands.JedisCommands

@Repository
class PrintingRepository(
    private val jedis: JedisCommands,
    private val adminConfigHelper: AdminConfigHelper
) {
    private val printingQueue = "printing_queue_${TaskType.STYLED_IMAGE}"
    private val printerQueuedJobCount = "printer_queued_job_count_${TaskType.STYLED_IMAGE}"

    fun addTaskToPrintingQueue(task: Task, fromConsole: Boolean = false): Printing {
        val printingName = "printing:${task.name}"

        if (!fromConsole) {
            val existingPrinting = jedis.get(printingName)
            if (existingPrinting != null) {
                throw DuplicatePrintException(
                    "Printing Task already exist for name: $printingName, value: $existingPrinting"
                )
            }
        }

        val printing = Printing(
            id = IdUtils.generateId(),
            name = printingName,
            task = task,
            status = PrintingStatus.READY,
        )
        val value = ObjectMapperUtils.toJSON(printing)

        jedis.set(printingName, value)
        log.debug("Set printing in Redis: ${printing.name}, value: $value")

        jedis.lpush(printingQueue, printingName)
        log.info("Lpush printing in Redis queue: ${printing.name}, printingName: $printingName")

        return printing
    }

    private fun calculateAheadCount(status: PrintingStatus, printingName: String): Int {
        return when (status) {
            PrintingStatus.READY -> getPrinterQueuedJobCount() + calculateAheadCountInPrintingQueue(printingName)
            PrintingStatus.QUEUING -> getPrinterQueuedJobCount()
            PrintingStatus.PRINTING -> 0
            else -> -1
        }
    }

    private fun calculateAheadCountInPrintingQueue(printingName: String): Int {
        val allElements = jedis.lrange(printingQueue, 0, -1)
        val index = allElements.indexOf(printingName)

        return if (index >= 0) allElements.size - 1 - index else -1
    }

    fun pollOneFromPrintingQueue(): Printing? {
        val printerQueuedJobCount = getPrinterQueuedJobCount()
        val adminConfig = adminConfigHelper.getAdminConfig()
        if (printerQueuedJobCount > adminConfig.maxPrinterJobCount) {
            return null
        }
        val printingName = jedis.rpop(printingQueue) ?: return null
        log.info("Rpop printingName from Redis queue: $printingName")

        val value = jedis.get(printingName) ?: return null
        log.info("Get printing value from Redis: $printingName, value: $value")

        val printing = ObjectMapperUtils.fromJSON(value, Printing::class.java)
        if (printing == null) {
            return printing
        }

        val newPrinting = printing.copy(
            status = PrintingStatus.QUEUING
        )
        val newValue = ObjectMapperUtils.toJSON(newPrinting)

        jedis.set(printingName, newValue)
        log.info("Update printing status in Redis: ${printing.name}, value: $newValue")

        return newPrinting
    }

    fun getPrinting(printingName: String): Printing {
        val value = jedis.get(printingName)
            ?: throw IllegalArgumentException("$printingName is not exists")
        log.debug("Get printing value from Redis: $printingName, value: $value")
        val printing = ObjectMapperUtils.fromJSON(value, Printing::class.java)!!
        return printing.copy(
            aheadCount = calculateAheadCount(printing.status, printingName)
        )
    }

    fun updatePrintingStatus(name: String, status: PrintingStatus): Printing {
        val printing = getPrinting(name)
        val newPrinting = printing.copy(status= status)
        val newValue = ObjectMapperUtils.toJSON(newPrinting)
        jedis.set(name, newValue)

        log.info("Updated printing: $name, newValue: $newValue")
        return newPrinting
    }

    fun queryAll(keyword: String): List<Printing> {
        val printingNames = jedis.lrange(printingQueue, 0, -1).reversed()
        val finalNames = if (keyword.isNotEmpty()) {
            printingNames.filter { it.contains(keyword) }
        } else {
            printingNames
        }
        log.debug("Query all with finalNames: {}", finalNames)
        return finalNames.map {
            getPrinting(it)
        }
    }

    fun getPrinterQueuedJobCount(): Int {
        return jedis.get(printerQueuedJobCount)?.toInt() ?: 0
    }

    fun setPrinterQueuedJobCount(request: SetPrinterQueuedJobCountRequest): String {
        return jedis.set(printerQueuedJobCount, request.printerQueuedJobCount.toString())
    }
}