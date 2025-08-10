package com.kling.waic.component.helper

import com.kling.waic.component.utils.Slf4j.Companion.log
import nu.pattern.OpenCV
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Component
@ConditionalOnProperty(
    name = ["waic.crop-image-with-opencv"],
    havingValue = "true",
    matchIfMissing = false
)
class ImageCropHelper(
    private val cascadeClassifiers: List<CascadeClassifier>,
) {
    init {
        // Load OpenCV native library
        OpenCV.loadLocally()
    }

    fun cropFaceToAspectRatio(inputImage: BufferedImage,
                              taskName: String,
                              cropRatio: Double): BufferedImage {
        // Convert BufferedImage to Mat
        val mat = bufferedImageToMat(inputImage)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        
        // Image preprocessing - histogram equalization to enhance contrast
        val equalizedGray = Mat()
        Imgproc.equalizeHist(gray, equalizedGray)

        val faces = MatOfRect()
        
        // Using multiple parameter combinations to improve detection success rate
        val detectionParams = listOf(
            // 参数组合1: 标准检测
            Triple(1.1, 3, 0.3),
            // 参数组合2: 更敏感的检测
            Triple(1.05, 2, 0.25),
            // 参数组合3: 更宽松的检测
            Triple(1.2, 4, 0.2),
            // 参数组合4: 最宽松的检测
            Triple(1.3, 1, 0.15)
        )
        
        var detectedFaces: Array<Rect> = emptyArray()
        var successfulClassifierIndex = -1
        
        // Loop through all classifiers
        for ((classifierIndex, cascadeClassifier) in cascadeClassifiers.withIndex()) {
            log.debug("Trying classifier ${classifierIndex + 1}/${cascadeClassifiers.size} for task: $taskName")
            
            for ((paramIndex, paramTriple) in detectionParams.withIndex()) {
                val (scaleFactor, minNeighbors, minSizeRatio) = paramTriple
                val minSize = org.opencv.core.Size(
                    (mat.width() * minSizeRatio).toInt().toDouble(),
                    (mat.height() * minSizeRatio).toInt().toDouble()
                )
                
                // First try detection on the equalized image
                cascadeClassifier.detectMultiScale(
                    equalizedGray, 
                    faces, 
                    scaleFactor, 
                    minNeighbors, 
                    0, 
                    minSize
                )
                
                detectedFaces = faces.toArray()
                if (detectedFaces.isNotEmpty()) {
                    successfulClassifierIndex = classifierIndex
                    log.info("Face detected with classifier $classifierIndex (equalized image) " +
                            "- param set $paramIndex: scaleFactor=$scaleFactor, minNeighbors=$minNeighbors, " +
                            "minSizeRatio=$minSizeRatio, faces=${detectedFaces.size}")
                    break
                }
                
                // Second try detection on the original gray image
                cascadeClassifier.detectMultiScale(
                    gray, 
                    faces, 
                    scaleFactor, 
                    minNeighbors, 
                    0, 
                    minSize
                )
                
                detectedFaces = faces.toArray()
                if (detectedFaces.isNotEmpty()) {
                    successfulClassifierIndex = classifierIndex
                    log.info("Face detected with classifier $classifierIndex (original gray) " +
                            "- param set $paramIndex: scaleFactor=$scaleFactor, minNeighbors=$minNeighbors, " +
                            "faces=${detectedFaces.size}")
                    break
                }
            }
            
            // If any classifier detected faces, stop trying others
            if (detectedFaces.isNotEmpty()) {
                break
            }
        }

        val faceCenter = if (detectedFaces.isNotEmpty()) {
            val face = detectedFaces.maxByOrNull { it.width * it.height } ?: detectedFaces[0]
            log.debug("Selected face for task $taskName (classifier $successfulClassifierIndex): " +
                    "x=${face.x}, y=${face.y}, width=${face.width}, height=${face.height}")

            Point(face.x + face.width / 2.0, face.y + face.height / 2.0)
        } else {
            log.warn("No face detected with any classifier and parameter combination for task: $taskName, " +
                    "image size: ${mat.width()}x${mat.height()}, tried ${cascadeClassifiers.size} classifiers, " +
                    "using center of original image as fallback")
            // Return the center of original image if no face detected
            Point(mat.width() / 2.0, mat.height() / 2.0)
        }

        // Calculate the maximum possible crop area with 2:3 aspect ratio
        val imageWidth = mat.width()
        val imageHeight = mat.height()

        val maxCropWidth: Int
        val maxCropHeight: Int

        val originalRatio = imageWidth.toDouble() / imageHeight.toDouble()

        if (originalRatio > cropRatio) {
            // Original image is wider, constrain by height to get maximum height crop
            maxCropHeight = imageHeight
            maxCropWidth = (maxCropHeight * cropRatio).toInt()
        } else {
            // Original image is taller, constrain by width to get maximum width crop
            maxCropWidth = imageWidth
            maxCropHeight = (maxCropWidth / cropRatio).toInt()
        }

        // Use face center as reference, but prioritize maximum size
        var cropX = (faceCenter.x - maxCropWidth / 2.0).toInt()
        var cropY = (faceCenter.y - maxCropHeight / 2.0).toInt()

        // Adjust to stay within boundaries while ensuring maximum size
        cropX = maxOf(0, minOf(cropX, imageWidth - maxCropWidth))
        cropY = maxOf(0, minOf(cropY, imageHeight - maxCropHeight))

        val rect = Rect(cropX, cropY, maxCropWidth, maxCropHeight)
        val cropped = Mat(mat, rect)

        // Convert Mat back to BufferedImage
        val bufferedImage = matToBufferedImage(cropped)
        log.debug("Cropped Image generated, bufferedImage: {} x {}", bufferedImage.width, bufferedImage.height)
        return bufferedImage
    }

    private fun bufferedImageToMat(inputImage: BufferedImage): Mat {
        // Validate input
        if (inputImage.width <= 0 || inputImage.height <= 0) {
            throw IllegalArgumentException("Invalid BufferedImage: width=${inputImage.width}, height=${inputImage.height}")
        }

        // Ensure the image is in a format compatible with JPG
        val jpgCompatibleImage = ensureJpgCompatibleImage(inputImage)

        val byteArrayOutputStream = ByteArrayOutputStream()
        val writeSuccess = ImageIO.write(jpgCompatibleImage, "jpg", byteArrayOutputStream)
        if (!writeSuccess) {
            throw IllegalStateException("Failed to convert jpgCompatibleImage to JPG bytes")
        }

        val imageBytes = byteArrayOutputStream.toByteArray()

        if (imageBytes.isEmpty()) {
            throw IllegalStateException("Image conversion resulted in empty byte array")
        }

        val matOfByte = MatOfByte(*imageBytes)
        val mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR)

        if (mat.empty()) {
            throw IllegalStateException("OpenCV failed to decode image bytes. Image may be corrupted or in unsupported format.")
        }

        return mat
    }

    fun ensureJpgCompatibleImage(inputImage: BufferedImage): BufferedImage {
        return if (inputImage.type == BufferedImage.TYPE_3BYTE_BGR) {
            inputImage
        } else {
            val outputImage = BufferedImage(
                inputImage.width,
                inputImage.height,
                BufferedImage.TYPE_3BYTE_BGR
            )

            val g = outputImage.createGraphics()
            g.drawImage(inputImage, 0, 0, null)
            g.dispose()

            log.info("Converted inputImage to TYPE_3BYTE_BGR for JPG compatibility, " +
                    "inputImage size: {} x {}, outputImage size: {} x {}",
                inputImage.width, inputImage.height, outputImage.width, inputImage.height)
            outputImage
        }
    }

    private fun matToBufferedImage(mat: Mat): BufferedImage {
        if (mat.empty()) {
            throw IllegalArgumentException("Cannot convert empty Mat to BufferedImage")
        }

        val matOfByte = MatOfByte()
        val encodeSuccess = Imgcodecs.imencode(".jpg", mat, matOfByte)

        if (!encodeSuccess) {
            throw IllegalStateException("Failed to encode Mat to JPG bytes")
        }

        val byteArray = matOfByte.toArray()

        if (byteArray.isEmpty()) {
            throw IllegalStateException("Mat encoding resulted in empty byte array")
        }

        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val bufferedImage = ImageIO.read(byteArrayInputStream)

        if (bufferedImage == null) {
            throw IllegalStateException("Failed to create BufferedImage from byte array")
        }

        return bufferedImage
    }
}
