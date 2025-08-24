package com.kling.waic.component.utils

import java.awt.Image
import java.awt.image.BufferedImage

object ImageUtils {
    fun resizeAndCropToRatio(
        originalImage: BufferedImage,
        targetWidth: Int,
        targetHeight: Int
    ): BufferedImage {
        val targetRatio = targetWidth.toDouble() / targetHeight
        val originalRatio = originalImage.width.toDouble() / originalImage.height

        var scaledWidth: Int
        var scaledHeight: Int

        if (originalRatio > targetRatio) {
            // 原图过宽 -> 高度对齐，宽度超出后裁掉
            scaledHeight = targetHeight
            scaledWidth = (targetHeight * originalRatio).toInt()
        } else {
            // 原图过高 -> 宽度对齐，高度超出后裁掉
            scaledWidth = targetWidth
            scaledHeight = (targetWidth / originalRatio).toInt()
        }

        // 先缩放
        val temp = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
        val g2dTemp = temp.createGraphics()
        g2dTemp.drawImage(
            originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH),
            0,
            0,
            null
        )
        g2dTemp.dispose()

        // 再居中裁剪成目标大小
        val x = (scaledWidth - targetWidth) / 2
        val y = (scaledHeight - targetHeight) / 2
        return temp.getSubimage(x, y, targetWidth, targetHeight)
    }
}