package com.kling.waic.helper

import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO


@Component
class S3Helper(
    private val s3Client: S3Client
) {

    fun upload(bucket: String, key: String, file: File): String {
        return doUpload(bucket, key,
            RequestBody.fromFile(file))
    }

    fun upload(bucket: String, key: String, file: MultipartFile): String {
        return doUpload(bucket, key,
            RequestBody.fromBytes(file.bytes))
    }

    fun uploadBufferedImage(
        bucket: String,
        key: String,
        bufferedImage: BufferedImage,
        format: String
    ): String {
        val bytes = bufferedImageToBytes(bufferedImage, format)
        val responseBody = RequestBody.fromBytes(bytes)
        return doUpload(
            bucket, key,
            responseBody
        )
    }

    private fun doUpload(
        bucket: String,
        key: String,
        requestBody: RequestBody
    ): String {
        val putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("image/jpeg")
            .contentDisposition("inline")
            .build()

        val response = s3Client.putObject(putRequest, requestBody)
        val fileUrl = "https://$bucket.s3.cn-north-1.amazonaws.com.cn/$key"
        log.info("File uploaded to S3, bucket: $bucket, key: $key, fileUrl: $fileUrl, response: $response")
        return fileUrl
    }


    @Throws(IOException::class)
    private fun bufferedImageToBytes(image: BufferedImage, format: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, format, baos) // format: "jpg", "png", etc.
        return baos.toByteArray()
    }
}