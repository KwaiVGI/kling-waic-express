package com.kling.waic.helper

import com.kling.waic.entity.Task
import com.kling.waic.utils.FileUtils
import com.kling.waic.utils.Slf4j.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import javax.imageio.ImageIO


@Component
class ImageProcessHelper(
    @param:Value("\${kling.proxy.host}") private val proxyHost: String,
    @param:Value("\${kling.proxy.port}") private val proxyPort: Int
) {

    suspend fun downloadAndCreateSudoku(
        task: Task,
        imageUrls:
        List<String>, outputPath: String
    ) {
        val images = withContext(Dispatchers.IO) {
            imageUrls.mapIndexed { index, url ->
                async {
                    log.info("Downloading image ${index + 1} from $url")
                    readImageWithProxy(url)
                }
            }.awaitAll().filterNotNull()
        }

        if (images.size != imageUrls.size) {
            throw IllegalStateException(
                "Some images could not be downloaded. Expected: ${imageUrls.size}, Actual: ${images.size}"
            )
        }

        log.info("Creating Sudoku image for task: ${task.name} at $outputPath")
        createKlingWAICSudokuImage(task, images, outputPath)
        log.info("Created image ${images.size} from $outputPath")
    }

    fun readImageWithProxy(url: String): BufferedImage? {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
        val url = URL(url)
        val connection = url.openConnection(proxy) as HttpURLConnection

        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        return connection.inputStream.use { inputStream ->
            ImageIO.read(inputStream)
        }
    }

    // check the logic
    private fun createKlingWAICSudokuImage(
        task: Task,
        images: List<BufferedImage>,
        outputPath: String
    ) {
        // 获取实际图片尺寸（假设所有图片尺寸相同）
        val actualImageWidth = images[0].width
        val actualImageHeight = images[0].height
        
        // 基准尺寸（原来的固定值）
        val baseCellWidth = 112
        val baseCellHeight = 168
        
        // 计算缩放倍数（取宽度和高度缩放倍数的平均值，或者选择其中一个）
        val scaleFactorWidth = actualImageWidth.toDouble() / baseCellWidth
        val scaleFactorHeight = actualImageHeight.toDouble() / baseCellHeight
        val scaleFactor = minOf(scaleFactorWidth, scaleFactorHeight) // 使用较小的倍数保持比例
        
        // 根据缩放倍数计算所有尺寸
        val cellWidth = (baseCellWidth * scaleFactor).toInt()
        val cellHeight = (baseCellHeight * scaleFactor).toInt()
        val gap = (0 * scaleFactor).toInt() // 原来是0，缩放后还是0
        
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
        
        // Logo 位置和尺寸也按比例缩放
        val logoTopLeftX = leftMargin
        val logoTopLeftY = topMargin + cellHeight * 3 + gap * 2 + (12 * scaleFactor).toInt()
        val logoImage = FileUtils.convertFileAsImage("KlingAI-logo-CN.png")
        val logoWidth = (59 * scaleFactor).toInt()
        val logoHeight = (18 * scaleFactor).toInt()
        val scaledLogoImage = logoImage.getScaledInstance(logoWidth, logoHeight, BufferedImage.SCALE_SMOOTH)
        g2d.drawImage(scaledLogoImage, logoTopLeftX, logoTopLeftY, null)
        
        // 文字位置和大小也按比例缩放
        val taskName = task.name
        val taskNameTopLeftX = leftMargin + (276 * scaleFactor).toInt()
        val taskNameTopLeftY = topMargin + cellHeight * 3 + gap * 2 + (26 * scaleFactor).toInt()
        
        g2d.color = Color.WHITE
        val fontSize = (12 * scaleFactor).toInt()
        g2d.font = Font("Arial", Font.PLAIN, fontSize)
        
        // 使用 TextLayout 处理文本渲染
        val font = g2d.font
        val frc = g2d.fontRenderContext
        val textLayout = TextLayout(taskName, font, frc)
        val textBounds = textLayout.bounds
        
        // 计算文本的最大X位置（右边距也按比例缩放）
        val textRightMargin = (22 * scaleFactor).toInt()
        val maxX = totalWidth - textRightMargin
        
        // 确保文本不超出最大宽度
        val drawX = minOf(taskNameTopLeftX, (maxX - textBounds.width).toInt())
        
        g2d.drawString(taskName, drawX, taskNameTopLeftY)
        
        g2d.dispose()
        ImageIO.write(canvas, "JPG", File(outputPath))
        log.info("Saved Sudoku image to $outputPath with scale factor: $scaleFactor (${actualImageWidth}x${actualImageHeight} -> ${cellWidth}x${cellHeight})")
    }
}