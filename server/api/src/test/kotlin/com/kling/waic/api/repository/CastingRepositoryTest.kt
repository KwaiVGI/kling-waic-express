package com.kling.waic.api.repository

import SpringBaseTest
import com.kling.waic.component.entity.*
import com.kling.waic.component.repository.CastingRepository
import com.kling.waic.component.repository.CodeGenerateRepository
import com.kling.waic.component.utils.IdUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.lang.Thread.sleep
import java.time.Instant
import kotlin.random.Random
import kotlin.test.assertEquals

class CastingRepositoryTest : SpringBaseTest() {

    @Autowired
    private lateinit var castingRepository: CastingRepository

    @Autowired
    private lateinit var codeGenerateRepository: CodeGenerateRepository

    @Test
    fun testAddToCastingQueue() {
        val total = 1
        val type = TaskType.STYLED_IMAGE
        val timestamp = Instant.now().toEpochMilli()

        val castings: MutableList<Casting> = mutableListOf()
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

            val casting = castingRepository.addToCastingQueue(task)
            castings.add(casting)
            sleep(500)
        }
        assertEquals(castings.size, total)
    }

    @Test
    fun testGetPinned() {
        val casting = castingRepository.getPinned(TaskType.STYLED_IMAGE)
        println(casting)
    }

    @Test
    fun testOperatePin() {
        val castingName = "casting:Test_No.100032"
        val result =
            castingRepository.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.PIN)
        assertTrue(result)
    }

    @Test
    fun testOperateUnPin() {
        val castingName = "casting:Test_No.100032"
        val result =
            castingRepository.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.UNPIN)
        assertTrue(result)
    }

    @Test
    fun testOperateDelete() {
        val castingName = "casting:Test_No.100032"
        val result =
            castingRepository.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.DELETE)
        assertTrue(result)
    }

    @Test
    fun testOperatePromote() {
        val castingName = "casting:Test_No.100040"
        val result =
            castingRepository.operate(TaskType.STYLED_IMAGE, castingName, TaskOperateAction.PROMOTE)
        assertTrue(result)
    }

    @Test
    fun testListWithoutKeyword() {
        var pageNum = 0
        var score: Double? = null
        val keyword = ""
        val result =
            castingRepository.list(TaskType.STYLED_IMAGE, keyword, score, 2, ++pageNum)
        assertEquals(result.castings.size, 2)
        var hasMore = result.hasMore
        score = result.score
        while (hasMore) {
            val result2 =
                castingRepository.list(TaskType.STYLED_IMAGE, keyword, score, 2, ++pageNum)
            hasMore = result2.hasMore
            score = result2.score
        }
    }

    @Test
    fun testListWithKeyword() {
        var pageNum = 0
        var score: Double? = null
        val keyword = "10004"
        val result =
            castingRepository.list(TaskType.STYLED_IMAGE, keyword, score, 2, ++pageNum)
        assertEquals(result.castings.size, 2)
        var hasMore = result.hasMore
        score = result.score
        while (hasMore) {
            val result2 =
                castingRepository.list(TaskType.STYLED_IMAGE, keyword, score, 2, ++pageNum)
            hasMore = result2.hasMore
            score = result2.score
        }
    }

    @Test
    fun testScreen() {
        val castings = castingRepository.screen(TaskType.STYLED_IMAGE, 1)
        println(castings.size)
    }
}