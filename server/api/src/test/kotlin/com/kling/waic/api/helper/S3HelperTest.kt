package com.kling.waic.api.helper

import SpringBaseTest
import com.kling.waic.component.helper.S3Helper
import com.kling.waic.component.utils.FileUtils
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