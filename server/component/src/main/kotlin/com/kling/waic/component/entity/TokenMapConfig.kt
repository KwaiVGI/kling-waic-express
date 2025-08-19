package com.kling.waic.component.entity

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "manage.token")
data class TokenMapConfig(
    val map: Map<String, String> = emptyMap()
)