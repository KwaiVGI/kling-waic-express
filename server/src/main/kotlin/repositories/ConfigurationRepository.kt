package com.klingai.express.repositories

import org.springframework.stereotype.Repository

@Repository
open class ConfigurationRepository(
    val openApiAccessKey: String = "",
    val openApiSecretKey: String = "",
    val managementAccessKey: String = "KlingAI_WAIC_Express_We_are_the_BEST"
) {
}
