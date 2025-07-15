package com.kling.waic.helper

import com.kling.waic.entity.Task
import com.kling.waic.utils.FileUtils
import com.kling.waic.utils.Slf4j.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO


@Component
class ImageProcessHelper {

    suspend fun downloadAndCreateSudoku(task: Task,
                                        imageUrls:
                                        List<String>, outputPath: String) {
        val images = withContext(Dispatchers.IO) {
            imageUrls.mapIndexed { index, url ->
                async {
                    log.info("Downloading image ${index + 1} from $url")
                    ImageIO.read(URL(url))
                }
            }.awaitAll().filterNotNull()
        }

        if (images.size != imageUrls.size) {
            throw IllegalStateException(
                "Some images could not be downloaded. Expected: ${imageUrls.size}, Actual: ${images.size}")
        }

        log.info("Creating Sudoku image for task: ${task.name} at $outputPath")
        createKlingWAICSudokuImage(task, images, outputPath)
        log.info("Created image ${images.size} from $outputPath")
    }

    // check the logic
    private fun createKlingWAICSudokuImage(task: Task,
                                           images: List<BufferedImage>,
                                           outputPath: String) {
        val cellWidth = 112
        val cellHeight = 168
        val gap = 0

        val topMargin = 24
        val bottomMargin = 42
        val leftMargin = 22
        val rightMargin = 22

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

            val scaledImage =
                images[i].getScaledInstance(cellWidth, cellHeight, BufferedImage.SCALE_SMOOTH)
            g2d.drawImage(scaledImage, x, y, null)
        }

        val logoTopLeftX = leftMargin
        val logoTopLeftY = topMargin + cellHeight * 3 + gap * 2 + 12
        val logoImage = FileUtils.convertFileAsImage("KlingAI-logo-CN.png")
        val scaledLogoImage =
            logoImage.getScaledInstance(59, 18, BufferedImage.SCALE_SMOOTH)
        g2d.drawImage(scaledLogoImage, logoTopLeftX, logoTopLeftY, null)

        val taskName = task.name
        val taskNameTopLeftX = leftMargin + 276
        val taskNameTopLeftY = topMargin + cellHeight * 3 + gap * 2 + 26

        g2d.color = Color.WHITE
        g2d.font = Font("Arial", Font.PLAIN, 12)
        g2d.drawString(taskName, taskNameTopLeftX, taskNameTopLeftY)

        g2d.dispose()
        ImageIO.write(canvas, "JPG", File(outputPath))
    }
}