package com.kling.waic.test.helper

import com.kling.waic.entity.Casting
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskOperateAction
import com.kling.waic.entity.TaskStatus
import com.kling.waic.entity.TaskType
import com.kling.waic.helper.CastingHelper
import com.kling.waic.repository.CodeGenerateRepository
import com.kling.waic.test.SpringBaseTest
import com.kling.waic.utils.IdUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.random.Random
import kotlin.test.assertEquals

class CastingHelperTest : SpringBaseTest() {

    @Autowired
    private lateinit var castingHelper: CastingHelper
    @Autowired
    private lateinit var codeGenerateRepository: CodeGenerateRepository

    @Test
    fun testAddToCastingQueue() {
        val total = 10
        val type = TaskType.STYLED_IMAGE
        val timestamp = Instant.now().toEpochMilli()

        val castings: MutableList<Casting> = mutableListOf()
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

            val casting = castingHelper.addToCastingQueue(task)
            castings.add(casting)
        }
        assertEquals(castings.size, total)
    }

    @Test
    fun testGetPinned() {
        val casting = castingHelper.getPinned(TaskType.STYLED_IMAGE)
        println(casting)
    }

    @Test
    fun testOperatePin() {
        val castingName = "casting:Test_No.100032"
        val result = castingHelper.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.PIN)
        assertTrue(result)
    }

    @Test
    fun testOperateUnPin() {
        val castingName = "casting:Test_No.100032"
        val result = castingHelper.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.UNPIN)
        assertTrue(result)
    }

    @Test
    fun testOperateDelete() {
        val castingName = "casting:Test_No.100032"
        val result = castingHelper.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.DELETE)
        assertTrue(result)
    }

    @Test
    fun testOperatePromote() {
        val castingName = "casting:Test_No.100035"
        val result = castingHelper.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.PROMOTE)
        assertTrue(result)
    }
}