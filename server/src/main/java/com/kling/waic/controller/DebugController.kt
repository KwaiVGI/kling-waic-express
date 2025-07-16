package com.kling.waic.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

//@RestController
@RequestMapping("/debug")
class DebugController(
    @Value("\${waic.sudoku.images-dir}")
    private val sudokuImagesDir: String,
    @Value("\${waic.sudoku.url-prefix}")
    private val sudokuUrlPrefix: String,
    @Value("\${jedis.password}")
    private val redisPass: String,
    @Value("\${waic.management.token}")
    private val managementToken: String,
    @Value("\${waic.open-api.access-key}")
    private val accessKey: String,
    @Value("\${waic.open-api.secret-key}")
    private val secretKey: String
) {

    @GetMapping("/config")
    fun getConfig(): Map<String, Any> {
        val workingDir = System.getProperty("user.dir")
        val imagesDir = File(sudokuImagesDir)
        val files = if (imagesDir.exists()) {
            imagesDir.listFiles()?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }

        return mapOf(
            "workingDirectory" to workingDir,
            "sudokuImagesDir" to sudokuImagesDir,
            "sudokuUrlPrefix" to sudokuUrlPrefix,
            "imagesDirExists" to imagesDir.exists(),
            "imagesDirAbsolutePath" to imagesDir.absolutePath,
            "filesInImagesDir" to files,
            "redisPass" to redisPass,
            "managementToken" to managementToken,
            "accessKey" to accessKey,
            "secretKey" to secretKey,
        )
    }
}
