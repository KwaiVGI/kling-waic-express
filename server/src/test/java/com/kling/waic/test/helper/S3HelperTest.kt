package com.kling.waic.test.helper

import com.kling.waic.helper.S3Helper
import com.kling.waic.test.SpringBaseTest
import com.kling.waic.utils.FileUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class S3HelperTest : SpringBaseTest() {

    @Autowired
    private lateinit var s3Helper: S3Helper

    @Test
    fun testUpload() {
        val filename = "request-No.100228.jpg"
        val file = FileUtils.getFileFromResources(filename)
        val inputImageUrl = s3Helper.upload("kling-waic", "input-images/$filename", file)
        println(inputImageUrl)
    }
}