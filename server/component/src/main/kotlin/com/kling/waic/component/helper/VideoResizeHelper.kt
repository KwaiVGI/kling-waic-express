package com.kling.waic.component.helper

import com.kling.waic.component.utils.FrameUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

enum class ResizeMode {
    STRETCH,  // 强制拉伸
    FIT,      // 保持比例 + 黑边
    CROP      // 保持比例 + 裁剪
}

@Component
class VideoResizeHelper(
    private val s3Helper: S3Helper,
    private val aesCipherHelper: AESCipherHelper,
    @param:Value("\${S3_BUCKET_NAME:kling-waic}")
    private val bucket: String,
) {

    fun resizeVideoByUrl(
        taskName: String,
        inputUrl: String,
        targetWidth: Int,
        targetHeight: Int,
        mode: ResizeMode = ResizeMode.FIT
    ): String {
        // 创建临时文件用于下载和处理
        val inputFile = File.createTempFile("input_video_", ".mp4")
        val outputFile = File.createTempFile("output_video_", ".mp4")
        
        try {
            // 1. 从URL下载视频文件
            log.debug("Downloading video from URL: $inputUrl")
            downloadFile(inputUrl, inputFile)
            
            // 2. 调用resizeVideo进行压缩处理，使用225:400比例
            log.debug("Resizing video with dimensions: ${targetWidth}x${targetHeight}, mode: $mode")
            resizeVideo(inputFile.absolutePath, outputFile.absolutePath, targetWidth, targetHeight, mode)
            
            // 3. 上传处理后的视频到S3
            val encrypted = aesCipherHelper.encrypt("resized-$taskName")
            val outputFilename = "$taskName-$encrypted.mp4"

            val s3Key = "resized-videos/${outputFilename}"
            log.debug("Uploading resized video to S3, bucket: $bucket, key: $s3Key")
            val s3Url = s3Helper.upload(bucket, s3Key, outputFile)
            
            log.info("Video processing completed successfully, taskName: $taskName," +
                    "inputUrl: $inputUrl, targetWidth: $targetWidth, targetHeight: $targetHeight," +
                    "s3Url: $s3Url")
            return s3Url
            
        } catch (e: Exception) {
            log.error("Error processing video from URL: $inputUrl", e)
            throw RuntimeException("Failed to process video: ${e.message}", e)
        } finally {
            // 清理临时文件
            if (inputFile.exists()) {
                inputFile.delete()
            }
            if (outputFile.exists()) {
                outputFile.delete()
            }
        }
    }
    
    /**
     * 从URL下载文件到本地
     */
    private fun downloadFile(url: String, outputFile: File) {
        try {
            val website = URL(url)
            val rbc = Channels.newChannel(website.openStream())
            val fos = FileOutputStream(outputFile)
            fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
            fos.close()
            rbc.close()
            log.info("File downloaded successfully: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            log.error("Failed to download file from URL: $url", e)
            throw RuntimeException("Failed to download file: ${e.message}", e)
        }
    }

    private fun resizeVideo(
        inputPath: String,
        outputPath: String,
        targetWidth: Int,
        targetHeight: Int,
        mode: ResizeMode
    ) {
        val grabber = FFmpegFrameGrabber(inputPath)
        grabber.start()

        val recorder = FFmpegFrameRecorder(outputPath, targetWidth, targetHeight)
        recorder.videoCodec = avcodec.AV_CODEC_ID_H264
        recorder.format = "mp4"
        recorder.frameRate = grabber.frameRate
        recorder.start()

        var frame: Frame? = grabber.grabImage()
        while (frame != null) {
            val processedFrame = when (mode) {
                ResizeMode.STRETCH -> FrameUtils.stretch(frame, targetWidth, targetHeight)
                ResizeMode.FIT -> FrameUtils.fit(frame, targetWidth, targetHeight)
                ResizeMode.CROP -> FrameUtils.crop(frame, targetWidth, targetHeight)
            }
            recorder.record(processedFrame)
            frame = grabber.grabImage()
        }

        recorder.stop()
        grabber.stop()
    }
}