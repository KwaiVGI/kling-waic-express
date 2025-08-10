package repository

import SpringBaseTest
import com.kling.waic.component.entity.*
import com.kling.waic.component.repository.CodeGenerateRepository
import com.kling.waic.component.repository.PrintingRepository
import com.kling.waic.component.utils.IdUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.random.Random
import kotlin.test.assertEquals

class PrintingRepositoryTest : SpringBaseTest() {

    @Autowired
    private lateinit var printingRepository: PrintingRepository
    @Autowired
    private lateinit var codeGenerateRepository: CodeGenerateRepository

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
                input = TaskInput(
                    type = type,
                    image = "https://kling-waic.s3.cn-north-1.amazonaws.com.cn/sudoku-No.100014.jpg"
                ),
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

            val printing = printingRepository.addTaskToPrintingQueue(task)
            printings.add(printing)
        }
        assertEquals(printings.size, total)
    }

    @Test
    fun testPollOneFromPrintingQueue() {
        val printing = printingRepository.pollOneFromPrintingQueue()
        println(printing)
    }

    @Test
    fun testGetPrinting() {
        val printing = printingRepository.getPrinting("printing:Test_No.100029")
        println(printing)
    }

    @Test
    fun testUpdatePrintingStatus() {
        printingRepository.updatePrintingStatus("printing:Test_No.100022", PrintingStatus.QUEUING)
        printingRepository.updatePrintingStatus("printing:Test_No.100023", PrintingStatus.PRINTING)
        printingRepository.updatePrintingStatus("printing:Test_No.100024", PrintingStatus.COMPLETED)
    }

    @Test
    fun testQueryAll() {
        val allPrintings = printingRepository.queryAll("")
        println(allPrintings)
        val allPrintings2 = printingRepository.queryAll("10002")
        println(allPrintings2)

        val allPrintings3 = printingRepository.queryAll("xxxxxxx")
        println(allPrintings3)
    }
}