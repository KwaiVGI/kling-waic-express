package com.klingai.express.repositories

import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Repository

@Configuration
open class ConfigurationRepository(
    val openApiAccessKey: String = "",
    val openApiSecretKey: String = "",
    val managementAccessKey: String = ""
) {
}
