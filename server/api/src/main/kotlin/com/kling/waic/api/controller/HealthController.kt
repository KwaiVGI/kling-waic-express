package com.kling.waic.api.controller

import com.kling.waic.component.entity.Result
import com.kling.waic.component.utils.FileUtils
import com.kling.waic.component.utils.ObjectMapperUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/health")
class HealthController {

    @GetMapping("/heartbeat")
    fun heartbeat(): Result<Map<String, Any>> {
        val response = mapOf(
            "status" to "ok",
            "timestamp" to Instant.now().toString(),
            "service" to "kling-waic-express"
        )
        return Result(response)
    }

    @GetMapping("/cors-test")
    fun corsTest(): Result<Map<String, String>> {
        val response = mapOf(
            "message" to "CORS is working correctly",
            "timestamp" to Instant.now().toString()
        )
        return Result(response)
    }

    // todo: remove this api, it's only for debug purpose
    @GetMapping("/file-read")
    fun fileRead(@RequestParam file: String = ""): Result<String> {
        val image = FileUtils.convertFileAsImage(file)
        return Result(image.toString())
    }
}