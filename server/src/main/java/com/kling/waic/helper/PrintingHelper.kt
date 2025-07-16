package com.kling.waic.helper

import com.kling.waic.entity.Printing
import com.kling.waic.entity.PrintingStatus
import com.kling.waic.entity.Task
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import redis.clients.jedis.Jedis
import java.util.UUID

@Component
class PrintingHelper(
    private val jedis: Jedis,
) {
    private val printingQueue = "printing_image_queue"

    fun addTaskToPrintingQueue(task: Task): Printing {
        val printingName = "printing:${task.name}"
        val printing = Printing(
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
            name = printingName,
            task = task,
            status = PrintingStatus.SUBMITTED
        )
        val value = ObjectMapperUtils.toJSON(printing)

        jedis.set(printingName, value)
        log.info("Set printing in Redis: ${printing.name}, value: $value")

        jedis.lpush(printingQueue, printingName)
        log.info("Lpush printing in Redis queue: ${printing.name}, printingName: $printingName")

        return printing
    }

    fun pollOneFromPrintingQueue(): Printing? {
        val printingName = jedis.rpop(printingQueue) ?: return null
        log.info("Rpop printingName from Redis queue: $printingName")

        val value = jedis.get(printingName) ?: return null
        log.info("Get printing value from Redis: $printingName, value: $value")

        return ObjectMapperUtils.fromJSON(value, Printing::class.java)
    }

    fun updatePrintingStatus(name: String, status: PrintingStatus): Printing {
        val value = jedis.get(name)
            ?: throw IllegalArgumentException("$name is not exists")
        log.info("Get printing value from Redis: $name, value: $value")

        val printing = ObjectMapperUtils.fromJSON(value, Printing::class.java)!!
        val newPrinting = printing.copy(status= status)
        val newValue = ObjectMapperUtils.toJSON(newPrinting)

        log.info("Updated printing: $name, newValue: $newValue")
        return newPrinting
    }
}