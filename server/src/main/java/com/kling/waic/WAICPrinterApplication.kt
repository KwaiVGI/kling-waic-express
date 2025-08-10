package com.kling.waic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.kling.waic.printer"]
)
open class WAICPrinterApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WAICPrinterApplication>(*args)
        }
    }
}