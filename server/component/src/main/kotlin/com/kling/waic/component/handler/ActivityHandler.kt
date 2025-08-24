package com.kling.waic.component.handler

import com.kling.waic.component.utils.Constants
import com.kling.waic.component.utils.FileUtils
import com.kling.waic.component.utils.ImageUtils
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

interface ActivityHandler {

    fun activityName(): String

    fun getCanvas(totalWidth: Int, totalHeight: Int): Pair<BufferedImage, Graphics2D>
}

@Component
class DefaultActivityHandler: ActivityHandler {

    override fun activityName(): String {
        return Constants.DEFAULT_ACTIVITY
    }

    override fun getCanvas(
        totalWidth: Int,
        totalHeight: Int
    ): Pair<BufferedImage, Graphics2D> {
        val canvas = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB)
        val g2d: Graphics2D = canvas.createGraphics()

        g2d.color = Color.BLACK
        g2d.fillRect(0, 0, totalWidth, totalHeight)
        return Pair(canvas, g2d)
    }
}

@Component
class XiaozhaoActivityHandler: ActivityHandler {

    override fun activityName(): String {
        return "xiaozhao"
    }

    override fun getCanvas(
        totalWidth: Int,
        totalHeight: Int
    ): Pair<BufferedImage, Graphics2D> {
        val wallpaperImage =
            FileUtils.getImageFromResources("Kuaishou-recruitment-background.png")
        val canvas =
            ImageUtils.resizeAndCropToRatio(wallpaperImage, totalWidth, totalHeight)
        val g2d: Graphics2D = canvas.createGraphics()
        return Pair(canvas, g2d)
    }
}