package com.kling.waic.test.helper

import com.kling.waic.entity.Printing
import com.kling.waic.entity.PrintingStatus
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskStatus
import com.kling.waic.entity.TaskType
import com.kling.waic.helper.PrintingHelper
import com.kling.waic.repository.CodeGenerateRepository
import com.kling.waic.test.SpringBaseTest
import com.kling.waic.utils.IdUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.Instant
import kotlin.random.Random
import kotlin.test.assertEquals

class PrintingHelperTest : SpringBaseTest() {

    @Autowired
    private lateinit var printingHelper: PrintingHelper
    @Autowired
    private lateinit var codeGenerateRepository: CodeGenerateRepository

    @Value("\${jedis.host}")
    private lateinit var redisHost: String

    @Value("\${jedis.port}")
    private var redisPort: Int = 0

    @Test
    fun testRedisConfig() {
        println("Redis Host: $redisHost")
        println("Redis Port: $redisPort")
        // 验证是否使用了测试配置
        assertEquals("172.28.199.165", redisHost)
        assertEquals(6389, redisPort)
    }

    @Test
    fun testAddTaskToPrintingQueue() {
        val total = 10
        val timestamp = Instant.now().toEpochMilli()
        val type = TaskType.STYLED_IMAGE

        val printings: MutableList<Printing> = mutableListOf()
        for (i in 1..total) {
            val taskName = codeGenerateRepository.nextCode(type)
            val task = Task(
                id = IdUtils.generateId(),
                name = "Test_${taskName}",
                taskIds = listOf(
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                    (timestamp + Random.nextInt(100)).toString(),
                ),
                status = TaskStatus.SUBMITTED,
                type = type,
                filename = "test_image_$timestamp.jpg",
                createTime = Instant.now(),
                updateTime = Instant.now(),
            )

            val printing = printingHelper.addTaskToPrintingQueue(task)
            printings.add(printing)
        }
        assertEquals(printings.size, total)
    }

    @Test
    fun testPollOneFromPrintingQueue() {
        val printing = printingHelper.pollOneFromPrintingQueue()
        println(printing)
    }

    @Test
    fun testGetPrinting() {
        val printing = printingHelper.getPrinting("printing:Test_No.100022")
        println(printing)
    }

    @Test
    fun testUpdatePrintingStatus() {
        printingHelper.updatePrintingStatus("printing:Test_No.100022", PrintingStatus.QUEUING)
        printingHelper.updatePrintingStatus("printing:Test_No.100023", PrintingStatus.PRINTING)
        printingHelper.updatePrintingStatus("printing:Test_No.100024", PrintingStatus.COMPLETED)
    }
}