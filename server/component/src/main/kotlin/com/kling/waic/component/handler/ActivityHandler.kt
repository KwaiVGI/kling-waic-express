package com.kling.waic.component.handler

import com.kling.waic.component.entity.ActivityConfigProps
import com.kling.waic.component.entity.Locale
import com.kling.waic.component.utils.Constants
import com.kling.waic.component.utils.FileUtils
import com.kling.waic.component.utils.ImageUtils
import com.kling.waic.component.utils.ThreadContextUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.lang.IllegalStateException

abstract class ActivityHandler {

    @Value("\${WAIC_OPENAPI_ACCESS_KEY}")
    private lateinit var waicOpenApiAccessKey: String
    @Value("\${WAIC_OPENAPI_SECRET_KEY}")
    private lateinit var waicOpenApiSecretKey: String
    private lateinit var activityConfigProps: ActivityConfigProps

    abstract fun activityName(): String

    abstract fun getCanvas(totalWidth: Int, totalHeight: Int): Pair<BufferedImage, Graphics2D>

    abstract fun drawLogoInLeftCorner(
        locale: Locale,
        scaleFactor: Double,
        g2d: Graphics2D,
        logoTopLeftX: Int,
        logoTopLeftY: Int
    )

    fun getAksk(): Pair<String, String> {
        val activity = ThreadContextUtils.getActivity()
        if (activity.isEmpty()) {
            return Pair(waicOpenApiAccessKey, waicOpenApiSecretKey)
        }

        val activityConfig = activityConfigProps.map[activity]
            ?: throw IllegalStateException("Activity config not found: $activity")
        return Pair(activityConfig.accessKey, activityConfig.secretKey)
    }
}

@Component
class DefaultActivityHandler: ActivityHandler() {

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

    override fun drawLogoInLeftCorner(
        locale: Locale,
        scaleFactor: Double,
        g2d: Graphics2D,
        logoTopLeftX: Int,
        logoTopLeftY: Int
    ) {
        val logoImage = FileUtils.convertFileAsImage("KlingAI-logo-$locale.png")
        val logoWidth = (67.5 * scaleFactor).toInt()
        val logoHeight = (18 * scaleFactor).toInt()
        val scaledLogoImage = logoImage.getScaledInstance(logoWidth, logoHeight, BufferedImage.SCALE_SMOOTH)
        g2d.drawImage(scaledLogoImage, logoTopLeftX, logoTopLeftY, null)
    }
}

@Component
class XiaozhaoActivityHandler: ActivityHandler() {

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

    override fun drawLogoInLeftCorner(
        locale: Locale,
        scaleFactor: Double,
        g2d: Graphics2D,
        logoTopLeftX: Int,
        logoTopLeftY: Int
    ) {
        val logoImage = FileUtils.convertFileAsImage("Kuaishou-Kling-logo-CN.png")
        val logoWidth = (200.4375 * scaleFactor).toInt()
        val logoHeight = (18 * scaleFactor).toInt()
        val scaledLogoImage = logoImage.getScaledInstance(logoWidth, logoHeight, BufferedImage.SCALE_SMOOTH)
        g2d.drawImage(scaledLogoImage, logoTopLeftX, logoTopLeftY, null)
    }
}