package com.kling.waic.component.utils

import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

object FrameUtils {
    private val converter = Java2DFrameConverter()

    fun stretch(frame: Frame, width: Int, height: Int): Frame {
        val img = converter.convert(frame)
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
        val g = scaled.createGraphics()
        g.drawImage(img, 0, 0, width, height, null)
        g.dispose()
        return converter.convert(scaled)
    }

    fun fit(frame: Frame, width: Int, height: Int): Frame {
        val img = converter.convert(frame)
        val ratio = img.width.toDouble() / img.height
        val targetRatio = width.toDouble() / height

        var scaledW: Int
        var scaledH: Int
        if (ratio > targetRatio) {
            scaledW = width
            scaledH = (width / ratio).toInt()
        } else {
            scaledH = height
            scaledW = (height * ratio).toInt()
        }

        val scaled = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
        val g: Graphics2D = scaled.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)
        g.drawImage(img, (width - scaledW) / 2, (height - scaledH) / 2, scaledW, scaledH, null)
        g.dispose()
        return converter.convert(scaled)
    }

    fun crop(frame: Frame, width: Int, height: Int): Frame {
        val img = converter.convert(frame)
        val ratio = img.width.toDouble() / img.height
        val targetRatio = width.toDouble() / height

        var scaledW: Int
        var scaledH: Int
        if (ratio > targetRatio) {
            scaledH = height
            scaledW = (height * ratio).toInt()
        } else {
            scaledW = width
            scaledH = (width / ratio).toInt()
        }

        val temp = BufferedImage(scaledW, scaledH, BufferedImage.TYPE_3BYTE_BGR)
        val g1: Graphics2D = temp.createGraphics()
        g1.drawImage(img, 0, 0, scaledW, scaledH, null)
        g1.dispose()

        val x = (scaledW - width) / 2
        val y = (scaledH - height) / 2
        val cropped = temp.getSubimage(x, y, width, height)
        return converter.convert(cropped)
    }
}