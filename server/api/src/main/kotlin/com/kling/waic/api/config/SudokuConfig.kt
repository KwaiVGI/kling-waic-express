package com.kling.waic.api.config

import com.kling.waic.component.utils.Slf4j.Companion.log
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
open class SudokuConfig(
    @Value("\${waic.sudoku.images-dir}")
    private val sudokuImagesDir: String,
    @Value("\${waic.sudoku.url-prefix}")
    private val sudokuUrlPrefix: String
) {

    @PostConstruct
    fun initSudokuDirectory() {
        val dir = File(sudokuImagesDir)
        if (!dir.exists()) {
            val created = dir.mkdirs()
            log.info("Created sudoku images directory: ${dir.absolutePath}, success: $created")
        }

        if (!dir.canWrite()) {
            throw IllegalStateException("Cannot write to sudoku images directory: ${dir.absolutePath}")
        }

        log.info("Sudoku images directory: ${dir.absolutePath}")
    }
}