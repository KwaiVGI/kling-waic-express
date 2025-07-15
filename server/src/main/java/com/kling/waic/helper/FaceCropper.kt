package com.kling.waic.helper

import nu.pattern.OpenCV
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Component
class FaceCropper(
    @Value("\${waic.sudoku.images-dir}")
    private val sudokuImagesDir: String,
    private val cascadeClassifier: CascadeClassifier,
) {
    init {
        // Load OpenCV native library
        OpenCV.loadLocally()
    }

    fun cropFaceToAspectRatio(inputImage: BufferedImage, taskName: String): BufferedImage {
        // Convert BufferedImage to Mat
        val mat = bufferedImageToMat(inputImage)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        val faces = MatOfRect()
        cascadeClassifier.detectMultiScale(gray, faces)

        if (faces.toArray().isEmpty()) {
            throw IllegalStateException("No human face detected in the image.")
        }

        // If multiple faces detected, select the largest one
        val face = faces.toArray().maxByOrNull { it.width * it.height } ?: faces.toArray()[0]
        val faceCenter = Point(face.x + face.width / 2.0, face.y + face.height / 2.0)

        // Calculate the maximum possible crop area with 2:3 aspect ratio
        val imageWidth = mat.width().toInt()
        val imageHeight = mat.height().toInt()
        val targetRatio = 2.0 / 3.0 // width:height = 2:3

        val maxCropWidth: Int
        val maxCropHeight: Int

        val originalRatio = imageWidth.toDouble() / imageHeight.toDouble()

        if (originalRatio > targetRatio) {
            // Original image is wider, constrain by height to get maximum height crop
            maxCropHeight = imageHeight
            maxCropWidth = (maxCropHeight * targetRatio).toInt()
        } else {
            // Original image is taller, constrain by width to get maximum width crop
            maxCropWidth = imageWidth
            maxCropHeight = (maxCropWidth / targetRatio).toInt()
        }

        // Use face center as reference, but prioritize maximum size
        var cropX = (faceCenter.x - maxCropWidth / 2.0).toInt()
        var cropY = (faceCenter.y - maxCropHeight / 2.0).toInt()

        // Adjust to stay within boundaries while ensuring maximum size
        cropX = maxOf(0, minOf(cropX, imageWidth - maxCropWidth))
        cropY = maxOf(0, minOf(cropY, imageHeight - maxCropHeight))

        val rect = Rect(cropX, cropY, maxCropWidth, maxCropHeight)
        val cropped = Mat(mat, rect)

        val outputPath = "$sudokuImagesDir/cropped-${taskName}.jpg"
        Imgcodecs.imwrite(outputPath, cropped)

        // Convert Mat back to BufferedImage
        return matToBufferedImage(cropped)
    }

    private fun bufferedImageToMat(bufferedImage: BufferedImage): Mat {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()

        val matOfByte = MatOfByte(*imageBytes)
        return Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR)
    }

    private fun matToBufferedImage(mat: Mat): BufferedImage {
        val matOfByte = MatOfByte()
        Imgcodecs.imencode(".jpg", mat, matOfByte)
        val byteArray = matOfByte.toArray()

        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        return ImageIO.read(byteArrayInputStream)
    }
}
