package com.kling.waic.helper

import com.kling.waic.entity.Printing
import com.kling.waic.entity.PrintingStatus
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskType
import com.kling.waic.exception.DuplicatePrintException
import com.kling.waic.utils.IdUtils
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import redis.clients.jedis.Jedis

@Component
class PrintingHelper(
    private val jedis: Jedis,
) {
    private val printingQueue = "printing_queue_${TaskType.STYLED_IMAGE}"

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
            status = PrintingStatus.SUBMITTED,
        )
        val value = ObjectMapperUtils.toJSON(printing)

        jedis.set(printingName, value)
        log.debug("Set printing in Redis: ${printing.name}, value: $value")

        jedis.lpush(printingQueue, printingName)
        log.info("Lpush printing in Redis queue: ${printing.name}, printingName: $printingName")

        return printing
    }

    private fun calculateAheadCount(printingName: String): Int {
        val allElements = jedis.lrange(printingQueue, 0, -1)
        val index = allElements.indexOf(printingName)

        return if (index >= 0) allElements.size - 1 - index else -1
    }

    fun pollOneFromPrintingQueue(): Printing? {
        val printingName = jedis.rpop(printingQueue) ?: return null
        log.info("Rpop printingName from Redis queue: $printingName")

        val value = jedis.get(printingName) ?: return null
        log.info("Get printing value from Redis: $printingName, value: $value")

        return ObjectMapperUtils.fromJSON(value, Printing::class.java)
    }

    fun getPrinting(name: String): Printing {
        val value = jedis.get(name)
            ?: throw IllegalArgumentException("$name is not exists")
        log.debug("Get printing value from Redis: $name, value: $value")
        val printing = ObjectMapperUtils.fromJSON(value, Printing::class.java)!!
        return printing.copy(
            aheadCount = calculateAheadCount(name)
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
}