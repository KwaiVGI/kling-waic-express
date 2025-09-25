package com.kling.waic.component.utils

import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.web.multipart.MultipartFile
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

class FileUtils {

    companion object {
        val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()

        fun readTextFromResources(filePath: String): String {
            val absoluteFile = File("/app/$filePath")
            if (absoluteFile.exists()) {
                return absoluteFile.readText()
            }
            return this::class.java.classLoader.getResource(filePath)?.readText()
                ?: throw IllegalArgumentException("File not found: $filePath")
        }

        fun readTextFromResourcesAsList(filePath: String): List<String> {
            return readTextFromResources(filePath)
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filter { !it.startsWith("#") }
                .toList()
        }

        fun getFileFromResources(filePath: String): File {
            val resource = this::class.java.classLoader.getResource(filePath)
                ?: throw IllegalArgumentException("File not found: $filePath")
            return File(resource.toURI())
        }

        fun getImageFromResources(filePath: String): BufferedImage {
            val resource = this::class.java.classLoader.getResourceAsStream(filePath)
                ?: throw IllegalArgumentException("File not found: $filePath")
            return resource.use { input ->
                ImageIO.read(input) ?: throw IOException("Unsupported image format: $filePath")
            }
        }

        fun readBytesFromResources(filePath: String): ByteArray {
            return this::class.java.classLoader.getResource(filePath)?.readBytes()
                ?: throw IllegalArgumentException("File not found: $filePath")
        }

        fun convertImageToBase64(filePath: String): String {
            val inputStream = this::class.java.classLoader.getResourceAsStream(filePath)
                ?: throw IllegalArgumentException("File not found: $filePath")
            return inputStream.use { stream ->
                BASE64_ENCODER.encodeToString(stream.readBytes())
            }
        }

        fun convertImageToBase64(file: File): String {
            return BASE64_ENCODER.encodeToString(file.readBytes())
        }

        fun convertImageToBase64(file: MultipartFile): String {
            return BASE64_ENCODER.encodeToString(file.bytes)
        }

        fun convertImageToBase64(bufferedImage: BufferedImage): String {
            val file = File.createTempFile("temp_image", ".jpg")
            ImageIO.write(bufferedImage, "jpg", file)
            return convertImageToBase64(file)
        }

        fun convertFileAsImage(filePath: String): Image {
            log.info("Converting file to image: $filePath")
            val inputStream = this::class.java.classLoader.getResourceAsStream(filePath)
                ?: throw IllegalArgumentException("File not found: $filePath")
            return inputStream.use { stream ->
                ImageIO.read(stream) ?: throw IOException("Can't read input file!")
            }
        }
    }
}