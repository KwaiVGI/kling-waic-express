package com.kling.waic.utils

import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.*

class FileUtils {

    companion object {
        val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

        fun readTextFromResources(filePath: String): String {
            return this::class.java.classLoader.getResource(filePath)?.readText()
                ?: throw IllegalArgumentException("File not found: $filePath")
        }

        fun readBytesFromResources(filePath: String): ByteArray {
            return this::class.java.classLoader.getResource(filePath)?.readBytes()
                ?: throw IllegalArgumentException("File not found: $filePath")
        }

        fun convertImageToBase64(filePath: String): String {
            val file = this::class.java.classLoader.getResource(filePath)?.file
                ?: throw IllegalArgumentException("File not found: $filePath")
            return BASE64_ENCODER.encodeToString(File(file).readBytes())
        }

        fun convertImageToBase64(file: File): String {
            return BASE64_ENCODER.encodeToString(file.readBytes())
        }

        fun convertImageToBase64(file: MultipartFile): String {
            return BASE64_ENCODER.encodeToString(file.bytes)
        }
    }
}