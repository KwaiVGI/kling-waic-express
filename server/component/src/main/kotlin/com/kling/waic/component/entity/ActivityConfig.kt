package com.kling.waic.component.entity

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "config.activity")
data class ActivityConfigProps(
    var map: Map<String, ActivityConfig> = emptyMap()
)

data class ActivityConfig(
    var token: String,
    var accessKey: String,
    var secretKey: String
)