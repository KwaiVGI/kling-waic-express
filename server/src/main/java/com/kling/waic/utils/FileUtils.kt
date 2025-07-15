package com.kling.waic.utils

import org.springframework.web.multipart.MultipartFile
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

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

        fun convertImageToBase64(bufferedImage: BufferedImage): String {
            val file = File.createTempFile("temp_image", ".png")
            ImageIO.write(bufferedImage, "png", file)
            return convertImageToBase64(file)
        }

        fun convertFileAsImage(filePath: String): Image {
            val file = this::class.java.classLoader.getResource(filePath)?.file
                ?: throw IllegalArgumentException("File not found: $filePath")
            return ImageIO.read(File(file))
        }
    }
}