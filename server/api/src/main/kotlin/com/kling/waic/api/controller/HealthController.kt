package com.kling.waic.api.controller

import com.kling.waic.component.entity.Result
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
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
}