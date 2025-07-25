package com.kling.waic.helper

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.kling.waic.entity.Locale
import com.kling.waic.utils.FileUtils
import com.kling.waic.utils.Slf4j.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import javax.imageio.ImageIO


@Component
class ImageProcessHelper(
    @param:Value("\${kling.proxy.host}") private val proxyHost: String,
    @param:Value("\${kling.proxy.port}") private val proxyPort: Int,
    @param:Value("\${kling.proxy.use-proxy}") private val useProxy: Boolean,
    @param:Value("\${s3.bucket}") private val bucket: String,
    private val s3Helper: S3Helper,
    private val aesCipherHelper: AESCipherHelper,
) {

    private fun validateImageFile(file: MultipartFile) {
        // Check if file is empty
        if (file.isEmpty) {
            throw IllegalArgumentException("Uploaded file is empty")
        }
        
        // Check file size (optional - you can set a reasonable limit)
        val maxSize = 50 * 1024 * 1024 // 50MB
        if (file.size > maxSize) {
            throw IllegalArgumentException("File size too large. Maximum allowed size is ${maxSize / (1024 * 1024)}MB")
        }
        
        // Check content type
        val contentType = file.contentType
        val supportedTypes = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", 
            "image/bmp", "image/webp", "image/tiff", "image/tif"
        )
        
        if (contentType != null && !supportedTypes.contains(contentType.lowercase())) {
            throw IllegalArgumentException("Unsupported image format: $contentType. " +
                    "Supported formats: ${supportedTypes.joinToString(", ")}")
        }
        
        // Check file extension
        val originalFilename = file.originalFilename
        if (originalFilename != null) {
            val extension = originalFilename.substringAfterLast('.', "").lowercase()
            val supportedExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif")
            if (extension.isNotEmpty() && !supportedExtensions.contains(extension)) {
                throw IllegalArgumentException("Unsupported file extension: .$extension. " +
                        "Supported extensions: ${supportedExtensions.joinToString(", ") { ".$it" }}")
            }
        }
    }

    fun multipartFileToBufferedImage(file: MultipartFile): BufferedImage {
        log.debug("Starting image processing for file: ${file.originalFilename}, size: ${file.size}, contentType: ${file.contentType}")
        
        // Validate file first
        validateImageFile(file)
        
        val inputStream = file.inputStream
        val originalImage = try {
            log.debug("Attempting to read image with ImageIO")
            val image = ImageIO.read(inputStream)
            log.info("ImageIO.read result: ${if (image != null) "success (${image.width}x${image.height})" else "null"}")
            image
        } catch (e: Exception) {
            log.error("Exception during ImageIO.read: ${e.message}", e)
            inputStream.close()
            throw IllegalArgumentException("Failed to read image data: ${e.message}")
        } finally {
            inputStream.close()
        }

        // Check if ImageIO could read the image
        if (originalImage == null) {
            log.error("ImageIO.read returned null for file: ${file.originalFilename}, contentType: ${file.contentType}, size: ${file.size}")
            throw IllegalArgumentException("Unable to read image file. " +
                    "The file may be corrupted or in an unsupported format. File name: ${file.originalFilename}, Content type: ${file.contentType}")
        }

        log.info("Successfully read image: ${originalImage.width}x${originalImage.height}, type: ${originalImage.type}")

        // Open the file as an InputStream to read EXIF data
        try {
            file.inputStream.use { exifInputStream ->
                val metadata = ImageMetadataReader.readMetadata(exifInputStream)
                val directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                val orientation = if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    directory.getInt(ExifIFD0Directory.TAG_ORIENTATION)
                } else {
                    1 // Default orientation if tag doesn't exist
                }

                log.debug("EXIF orientation: $orientation")
                return rotateImageIfNeeded(originalImage, orientation)
            }
        } catch (e: Exception) {
            log.warn("Failed to read EXIF data, using original image without rotation: ${e.message}")
            return originalImage
        }
    }

    fun rotateImageIfNeeded(image: BufferedImage?, orientation: Int): BufferedImage {
        // Safety check for null image
        if (image == null) {
            throw IllegalArgumentException("Cannot rotate null image")
        }
        
        val transform = AffineTransform()
        when (orientation) {
            6 -> transform.rotate(Math.toRadians(90.0), image.height / 2.0, image.height / 2.0)
            3 -> transform.rotate(Math.toRadians(180.0), image.width / 2.0, image.height / 2.0)
            8 -> transform.rotate(Math.toRadians(270.0), image.width / 2.0, image.width / 2.0)
            else -> return image // No need to rotate
        }

        val op = AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC)
        val newImage = BufferedImage(image.height, image.width, image.type)
        op.filter(image, newImage)
        return newImage
    }

    suspend fun downloadAndCreateSudoku(
        taskName: String,
        imageUrls:
        List<String>,
        locale: Locale
    ): String {
        val images = withContext(Dispatchers.IO) {
            imageUrls.mapIndexed { index, url ->
                async {
                    log.info("Downloading image ${index + 1} from $url")
                    readImageWithProxy(url)
                }
            }.awaitAll()
        }

        if (images.size != imageUrls.size) {
            throw IllegalStateException(
                "Some images could not be downloaded. Expected: ${imageUrls.size}, Actual: ${images.size}"
            )
        }

        val sudokuImage = createKlingWAICSudokuImage(taskName, images, locale)

        val outputFilename = aesCipherHelper.encrypt("sudoku-${taskName}") + ".jpg"
        val outputImageUrl = s3Helper.uploadBufferedImage(bucket,
            "output-images/$outputFilename",
            sudokuImage, "jpg")
        return outputImageUrl
    }

    fun readImageWithProxy(url: String): BufferedImage {
        val url = URL(url)
        val connection = if (useProxy) {
            log.info("Proxy connect to $url via $proxyHost:$proxyPort")
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            url.openConnection(proxy) as HttpURLConnection
        } else {
            log.info("Direct connect to $url")
            url.openConnection() as HttpURLConnection
        }

        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        return connection.inputStream.use { inputStream ->
            ImageIO.read(inputStream)
        }
    }

    // check the logic
    private fun createKlingWAICSudokuImage(
        taskName: String,
        images: List<BufferedImage>,
        locale: Locale
    ): BufferedImage {
        // Get actual image dimensions (assuming all images have the same size)
        val actualImageWidth = images[0].width
        val actualImageHeight = images[0].height
        
        // Base dimensions (original fixed values)
        val baseCellWidth = 112
        val baseCellHeight = 168
        
        // Calculate scale factors (take average of width and height scale factors, or choose one)
        val scaleFactorWidth = actualImageWidth.toDouble() / baseCellWidth
        val scaleFactorHeight = actualImageHeight.toDouble() / baseCellHeight
        val scaleFactor = minOf(scaleFactorWidth, scaleFactorHeight) // Use smaller factor to maintain aspect ratio
        
        // Calculate all dimensions based on scale factor
        val cellWidth = (baseCellWidth * scaleFactor).toInt()
        val cellHeight = (baseCellHeight * scaleFactor).toInt()
        val gap = (0 * scaleFactor).toInt() // Originally 0, still 0 after scaling
        
        val topMargin = (24 * scaleFactor).toInt()
        val bottomMargin = (42 * scaleFactor).toInt()
        val leftMargin = (22 * scaleFactor).toInt()
        val rightMargin = (22 * scaleFactor).toInt()
        
        val totalWidth = leftMargin + cellWidth * 3 + gap * 2 + rightMargin
        val totalHeight = topMargin + cellHeight * 3 + gap * 2 + bottomMargin
        
        val canvas = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB)
        val g2d: Graphics2D = canvas.createGraphics()
        
        g2d.color = Color.BLACK
        g2d.fillRect(0, 0, totalWidth, totalHeight)
        
        for (i in 0 until 9) {
            val row = i / 3
            val col = i % 3
            
            val x = leftMargin + col * (cellWidth + gap) + gap
            val y = topMargin + row * (cellHeight + gap) + gap
            
            val scaledImage = images[i].getScaledInstance(cellWidth, cellHeight, BufferedImage.SCALE_SMOOTH)
            g2d.drawImage(scaledImage, x, y, null)
        }
        
        // Logo position and size also scaled proportionally
        val logoTopLeftX = leftMargin
        val logoTopLeftY = topMargin + cellHeight * 3 + gap * 2 + (12 * scaleFactor).toInt()
        val logoImage = FileUtils.convertFileAsImage("KlingAI-logo-$locale.png")
        val logoWidth = (67 * scaleFactor).toInt()
        val logoHeight = (18 * scaleFactor).toInt()
        val scaledLogoImage = logoImage.getScaledInstance(logoWidth, logoHeight, BufferedImage.SCALE_SMOOTH)
        g2d.drawImage(scaledLogoImage, logoTopLeftX, logoTopLeftY, null)
        
        // Text position and size also scaled proportionally
        val taskNameTopLeftX = leftMargin + (276 * scaleFactor).toInt()
        val taskNameTopLeftY = topMargin + cellHeight * 3 + gap * 2 + (26 * scaleFactor).toInt()
        
        g2d.color = Color.WHITE
        val fontSize = (12 * scaleFactor).toInt()
        g2d.font = Font("Arial", Font.PLAIN, fontSize)
        
        // Use TextLayout to handle text rendering
        val font = g2d.font
        val frc = g2d.fontRenderContext
        val textLayout = TextLayout(taskName, font, frc)
        val textBounds = textLayout.bounds
        
        // Calculate maximum X position for text (right margin also scaled proportionally)
        val textRightMargin = (22 * scaleFactor).toInt()
        val maxX = totalWidth - textRightMargin
        
        // Ensure text does not exceed maximum width
        val drawX = minOf(taskNameTopLeftX, (maxX - textBounds.width).toInt())
        
        g2d.drawString(taskName, drawX, taskNameTopLeftY)
        
        g2d.dispose()
        return canvas
    }
}