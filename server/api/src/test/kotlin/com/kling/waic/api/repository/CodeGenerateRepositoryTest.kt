package com.kling.waic.api.repository

import SpringBaseTest
import com.kling.waic.component.entity.TaskType
import com.kling.waic.component.repository.CodeGenerateRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.lang.Thread.sleep

class CodeGenerateRepositoryTest : SpringBaseTest() {

    @Autowired
    private lateinit var codeGenerateRepository: CodeGenerateRepository

    @Test
    fun testGenerateNameForCreateImage() {
        for (i in 1..10) {
            codeGenerateRepository.nextCode(TaskType.STYLED_IMAGE).also {
                println("Generated code: $it")
            }
            sleep(1000)
        }
    }

    @Test
    fun testGenerateNameForCreateVideo() {
        for (i in 1..10) {
            codeGenerateRepository.nextCode(TaskType.VIDEO_EFFECT).also {
                println("Generated code: $it")
            }
            sleep(1000)
        }
    }
}