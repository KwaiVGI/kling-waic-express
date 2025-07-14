package com.kling.waic.helper

import com.kling.waic.utils.Slf4j.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO


@Component
class ImageProcessHelper {

    suspend fun downloadAndCreateSudoku(imageUrls: List<String>, outputPath: String) {
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

        createKlingWAICSudokuImage(images, outputPath)
        log.info("Created image ${images.size} from $outputPath")
    }

    // check the logic
    private fun createKlingWAICSudokuImage(images: List<BufferedImage>, outputPath: String) {
        val cellSize = 300
        val gridSize = cellSize * 3
        val gap = 10
        val totalSize = gridSize + gap * 2

        val canvas = BufferedImage(totalSize, totalSize, BufferedImage.TYPE_INT_RGB)
        val g2d: Graphics2D = canvas.createGraphics()

        g2d.color = java.awt.Color.WHITE
        g2d.fillRect(0, 0, totalSize, totalSize)

        for (i in 0 until 9) {
            val row = i / 3
            val col = i % 3

            val x = col * (cellSize + gap) + gap
            val y = row * (cellSize + gap) + gap

            val scaledImage =
                images[i].getScaledInstance(cellSize, cellSize, BufferedImage.SCALE_SMOOTH)
            g2d.drawImage(scaledImage, x, y, null)
        }

        g2d.dispose()

        ImageIO.write(canvas, "JPG", File(outputPath))
    }
}