package com.kling.waic.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.kling.waic"]
)
open class WAICExpressApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WAICExpressApplication>(*args)
        }
    }
}