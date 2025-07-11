package com.kling.waic.utils

import java.io.File
import java.util.Base64

class FileUtils {

    companion object {
        val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

        fun readFileFromResources(filePath: String): String {
            return this::class.java.classLoader.getResource(filePath)?.readText()
                ?: throw IllegalArgumentException("File not found: $filePath")
        }

        fun covertImageToBase64(filePath: String): String {
            val file = this::class.java.classLoader.getResource(filePath)?.file
                ?: throw IllegalArgumentException("File not found: $filePath")
            return BASE64_ENCODER.encodeToString(File(file).readBytes())
        }
    }
}